package org.zunpeng.vertx.kotlin

import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.zunpeng.vertx.kotlin.service.proxy.MyProxyService
import org.zunpeng.vertx.kotlin.service.proxy.MyProxyServiceFactory
import org.zunpeng.vertx.kotlin.service.proxy.MyProxyVerticle
import org.zunpeng.vertx.kotlin.service.proxy.User
import org.zunpeng.vertx.kotlin.service.proxy.kotlin.userOf

class MainVerticle : CoroutineVerticle() {

  companion object {
    private val logger = KotlinLogging.logger {}
  }

  override suspend fun start() {
    val deploymentId: String = awaitResult { handler -> vertx.deployVerticle(MyProxyVerticle::class.java.name, handler) }
    logger.info { "my proxy deployment id: $deploymentId" }

    val myProxyService: MyProxyService = MyProxyServiceFactory.createProxy(vertx, "my-proxy")

    val router: Router = Router.router(vertx)
    router.route("/1").handler { routingContext ->
      launch(context = routingContext.vertx().dispatcher()) {
        logger.info { "hello 1" }
        val jsonObject: JsonObject = awaitResult { handler -> myProxyService.get(json { obj("key" to "value") }, handler) }
        routingContext.response().end(jsonObject.encode())
      }
    }

    router.route("/2").handler { routingContext ->
      launch(context = routingContext.vertx().dispatcher()) {
        logger.info { "hello 2" }
        val user: User = awaitResult { handler -> myProxyService.getUser(userOf(id = 100L, sn = "snsnsnsn"), handler) }
        routingContext.response().end(user.toJson().encode())
      }
    }

    router.route().handler { routingContext ->
      routingContext.response().end("hello world")
    }

    val httpServer: HttpServer = vertx.createHttpServer(httpServerOptionsOf())
      .requestHandler(router)
      .listen(8886)
      .await()
    logger.info { "http server port: ${httpServer.actualPort()}" }
  }
}
