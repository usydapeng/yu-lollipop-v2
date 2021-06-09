package com.duobeiyun.feishu.service.mysql

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool

@VertxGen
@ProxyGen
interface MysqlService {

  /**
   * 按转换状态统计com环境课程的转换情况
   */
  @Fluent
  fun countByConvertStatusFromCom(query: JsonObject, handle: Handler<AsyncResult<JsonObject>>): MysqlService

  /**
   * 按转换状态统计net环境课程的转换情况
   */
  @Fluent
  fun countByConvertStatusFromNet(query: JsonObject, handle: Handler<AsyncResult<JsonObject>>): MysqlService
}
