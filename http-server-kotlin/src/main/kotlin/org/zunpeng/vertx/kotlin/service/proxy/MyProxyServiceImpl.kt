package org.zunpeng.vertx.kotlin.service.proxy

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import mu.KotlinLogging
import org.zunpeng.vertx.kotlin.service.proxy.kotlin.userOf

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

  override fun getUser(user: User, handler: Handler<AsyncResult<User>>): MyProxyService {
    logger.info { "user: ${user.toJson().encode()}" }
    handler.handle(Future.succeededFuture(userOf(id = 1000L, sn = "success-sn")))
    return this
  }
}
