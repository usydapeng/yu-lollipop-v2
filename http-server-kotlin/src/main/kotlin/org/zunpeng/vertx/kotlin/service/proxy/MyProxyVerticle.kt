package org.zunpeng.vertx.kotlin.service.proxy

import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.serviceproxy.ServiceBinder

class MyProxyVerticle : CoroutineVerticle() {

  override suspend fun start() {
    val myProxyService: MyProxyService = awaitResult { handler -> MyProxyServiceFactory.create(vertx, handler) }
    println(myProxyService.javaClass)
    ServiceBinder(vertx).setAddress("my-proxy").register(MyProxyService::class.java, myProxyService)
  }
}
