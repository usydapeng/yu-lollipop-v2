package com.duobeiyun.feishu.service.mysql

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging

class MysqlServiceImpl(
  private val vertx: Vertx,
  private val poolFromCom: Pool,
  private val poolFromNet: Pool,
  handle: Handler<AsyncResult<MysqlService>>
) : MysqlService {

  companion object {
    private val logger = KotlinLogging.logger {}
  }

  init {
    handle.handle(Future.succeededFuture(this))
  }

  override fun countByConvertStatusFromCom(query: JsonObject, handle: Handler<AsyncResult<JsonObject>>): MysqlService {
    val sql = """
      select convertStatus, count(*) as a_count from nebula_meeting.course_info
      where endTime between #{startTime} and #{endTime} group by convertStatus order by convertStatus asc
    """.trimIndent()
    countByConvertStatus(sql = sql, queryMap = query.map, pool = poolFromCom, handle = handle)
    return this
  }

  override fun countByConvertStatusFromNet(query: JsonObject, handle: Handler<AsyncResult<JsonObject>>): MysqlService {
    val sql = """
      select convert_status as convertStatus, count(*) as a_count from sail_platform_course.course
      where end_time between #{startTime} and #{endTime} group by convert_status order by convert_status asc
    """.trimIndent()
    countByConvertStatus(sql = sql, queryMap = query.map, pool = poolFromNet, handle = handle)
    return this
  }

  fun countByConvertStatus(
    sql: String,
    queryMap: Map<String, Any>,
    pool: Pool,
    handle: Handler<AsyncResult<JsonObject>>
  ) {
    SqlTemplate.forQuery(pool, sql)
      .mapTo(Row::toJson)
      .execute(queryMap)
      .onSuccess { rowSet ->
        if (rowSet == null || rowSet.size() == 0) {
          logger.info { "query: ${JsonObject(queryMap).encode()}, course not found" }
          handle.handle(Future.succeededFuture(JsonObject()))
        } else {
          val result = rowSet.associate {
            val convertStatus = when (it.getInteger("convertStatus", 0)) {
              -1 -> "default"
              0 -> "processing"
              1 -> "success"
              2 -> "failure"
              3 -> "empty"
              else -> {
                logger.error { "convert status not found" }
                "error"
              }
            }
            convertStatus to it.getLong("a_count", 0)
          }
          logger.info { "result: $result" }
          handle.handle(Future.succeededFuture(JsonObject(result)))
        }
      }.onFailure { throwable ->
        logger.error(throwable) { throwable.message }
        handle.handle(Future.failedFuture(throwable))
      }
  }
}
