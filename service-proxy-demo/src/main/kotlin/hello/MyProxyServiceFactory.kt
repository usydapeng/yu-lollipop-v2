package hello

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx

object MyProxyServiceFactory {

  fun create(vertx: Vertx, handler: Handler<AsyncResult<MyProxyService>>): MyProxyService =
    MyProxyServiceImpl(vertx, handler)

  // fun createProxy(vertx: Vertx, address: String, options: DeliveryOptions = DeliveryOptions()): MyProxyService =
}
