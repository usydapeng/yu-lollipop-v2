package com.duobeiyun.feishu.service.mysql

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.sqlclient.Pool

object MysqlServiceFactory {
  fun create(
    vertx: Vertx,
    poolFromCom: Pool,
    poolFromNet: Pool,
    handle: Handler<AsyncResult<MysqlService>>
  ): MysqlService =
    MysqlServiceImpl(vertx = vertx, poolFromCom = poolFromCom, poolFromNet = poolFromNet, handle = handle)

  fun createProxy(vertx: Vertx, address: String, options: DeliveryOptions = DeliveryOptions()): MysqlService =
    MysqlServiceVertxEBProxy(vertx, address, options)
}
