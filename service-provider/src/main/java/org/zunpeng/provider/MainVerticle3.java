package org.zunpeng.provider;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.circuitbreaker.CircuitBreaker;
import io.vertx.mutiny.circuitbreaker.HystrixMetricHandler;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import io.vertx.mutiny.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

public class MainVerticle3 extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MainVerticle3.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    CircuitBreakerOptions circuitBreakerOptions =
      new CircuitBreakerOptions()
        .setMaxFailures(2)
        .setMaxRetries(2)
        .setTimeout(2000)
        .setFallbackOnFailure(true)
        .setResetTimeout(10000);
    CircuitBreaker circuitBreaker = CircuitBreaker.create("my-curicuit-breaker", vertx, circuitBreakerOptions)
      .openHandler(() -> {
        logger.info("Circuit opened");
      }).closeHandler(() -> {
        logger.info("Circuit closed");
      });

    circuitBreaker.executeWithFallback(
      // vertx.createHttpClient(new HttpClientOptions().setSsl(true).setDefaultHost("www.baidu.com").setDefaultPort(443))
      vertx.createHttpClient(new HttpClientOptions().setSsl(true).setDefaultHost("www.tosee.tech").setDefaultPort(443))
        .request(HttpMethod.GET, "/sdsdsd")
        .onItem()
        .transformToUni((Function<HttpClientRequest, Uni<? extends HttpClientResponse>>) HttpClientRequest::send)
        .onItem()
        .transformToUni(resp -> {
          logger.info("----------");
          if (resp.statusCode() != 200) {
            return Uni.createFrom().failure(() -> new Exception("HttpError"));
          } else {
            return resp.body().map(Buffer::toString);
          }
        }),
      v -> {
        return "HelloWorld";
      }
    ).subscribe()
      .with(a -> {
        logger.info("result: {}", a);
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });

    Router router = Router.router(vertx);
    router.get("/hystrix-metrics").handler(HystrixMetricHandler.create(vertx));
    vertx.createHttpServer().requestHandler(router).listen(8080)
      .subscribe()
      .with(httpServer -> {
        logger.info("http server start: {}", httpServer.actualPort());
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });
  }
}
