package hello

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import mu.KotlinLogging

class MyProxyServiceImpl(
  private val vertx: Vertx,
  handler: Handler<AsyncResult<MyProxyService>>
) : MyProxyService {

  companion object {
    private val logger = KotlinLogging.logger {}
  }

  init {
    handler.handle(Future.succeededFuture(this))
  }

  override fun get(query: JsonObject, handler: Handler<AsyncResult<JsonObject>>): MyProxyService {
    logger.info { "query: ${query.encode()}" }
    handler.handle(Future.succeededFuture(query.put("proxy", "my")))
    return this;
  }
}
