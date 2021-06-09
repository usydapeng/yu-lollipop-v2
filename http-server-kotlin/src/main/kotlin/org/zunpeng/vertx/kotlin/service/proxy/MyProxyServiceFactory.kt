package org.zunpeng.vertx.kotlin.service.proxy

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions

object MyProxyServiceFactory {

  fun create(vertx: Vertx, handler: Handler<AsyncResult<MyProxyService>>): MyProxyService =
    MyProxyServiceImpl(vertx, handler)

  fun createProxy(vertx: Vertx, address: String, options: DeliveryOptions = DeliveryOptions()): MyProxyService =
    MyProxyServiceVertxEBProxy(vertx, address, options)
}
