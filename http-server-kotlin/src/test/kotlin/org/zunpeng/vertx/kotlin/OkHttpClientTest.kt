package org.zunpeng.vertx.kotlin

import io.vertx.core.Vertx
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request

class OkHttpClientTest {
  companion object {
    private val logger = KotlinLogging.logger {}
  }

  private var client: OkHttpClient = OkHttpClient()

  constructor() {
  }

  fun run(url: String): Int {
    val request = Request.Builder().url(url).build()
    return try {
      val response = client.newCall(request).execute()
      response.code
    } catch (t: Throwable) {
      logger.error(t.message, t)
      -1
    }
  }
}

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
  val vertx = Vertx.vertx()
  val clientTest = OkHttpClientTest()
  val url = "https://api.rzrtc.com/config/api/v1/media-server?appId=BrQl1o1wN3Kj8wDE&deviceId=NHG47K"
  for (i in 1..100) {
    val resp = clientTest.run(url)
    logger.info { "index: $i, resp: $resp" }
  }

  vertx.setPeriodic(1000*60*5) {
    val resp = clientTest.run(url)
    logger.info { "period, resp: $resp" }

  }
}
