package org.zunpeng.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

@ExtendWith(VertxExtension.class)
public class TestWebSocketClient {

  private static final Logger logger = LogManager.getLogger(TestWebSocketClient.class);

  @Test
  public void demo1(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
      .setDefaultHost("tinderdev-sio-bj.duobeiyun.com")
      .setDefaultPort(20081)
      .setSsl(true)
    );
    client.webSocket("/", res -> {
      if (res.succeeded()) {
        WebSocket ws = res.result();
        logger.info("succeeded!");
        testContext.completeNow();
      } else {
        logger.info("res: {}", res.failed());
        logger.info(res.cause().getMessage(), res.cause());
        testContext.failNow(res.cause());
      }
    });
  }

  @Test
  public void demo2(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient(
      new HttpClientOptions()
      .setDefaultHost("www.baidu.com")
      .setDefaultPort(80)
      .setSsl(false)
      .setLogActivity(true)
    );
    client.request(HttpMethod.GET, "/", ar -> {
      if (ar.failed()) {
        logger.error(ar.cause().getMessage(), ar.cause());
        testContext.failNow(ar.cause());
      } else {
        logger.info("----- build request -----");
        ar.result().send(rr -> {
          if (rr.failed()) {
            logger.error(rr.cause().getMessage(), rr.cause());
            testContext.failNow(rr.cause());
          } else {
            logger.info("----- resolve response -----");
            logger.info(rr.result().headers());
            rr.result().body(aa -> {
              if (aa.failed()) {
                logger.error(aa.cause().getMessage(), aa.cause());
                testContext.failNow(aa.cause());
              } else {
                logger.info("----- resolve response body -----");
                logger.info(aa.result().toString());
                logger.info("success!");
                testContext.completeNow();
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void demo3(Vertx vertx, VertxTestContext testContext) {
    io.vertx.mutiny.core.http.HttpClient client = new io.vertx.mutiny.core.Vertx(vertx).createHttpClient(
      new HttpClientOptions()
        .setDefaultHost("www.baidu.com")
        .setDefaultPort(80)
        .setSsl(false)
        .setLogActivity(true)
    );
    client.request(HttpMethod.GET, "/")
      .onItem()
      .transformToUni(request -> {
        logger.info("----- build request -----");
        return request.send();
      })
      .onItem()
      .transformToUni(response -> {
        logger.info("----- resolve response -----");
        logger.info(response.headers());
        return response.body();
      })
      .subscribe()
      .with(body -> {
        logger.info("body: {}", body);
        logger.info("success!");
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo3_1(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(HttpVerticle.class.getName(), new DeploymentOptions().setInstances(1), ar -> {
      if (ar.failed()) {
        logger.error(ar.cause().getMessage(), ar.cause());
      } else {
        logger.info("success");
      }
    });
  }

  @Test
  public void demo4(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(SockJsVerticle.class, new DeploymentOptions().setInstances(1), ar -> {
      if (ar.failed()) {
        logger.error("---------------------------------");
        logger.error(ar.cause().getMessage(), ar.cause());
      } else {
        logger.info("success");
      }
    });
  }

  public static class SockJsVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
      Router router = Router.router(vertx);

      Set<String> allowedHeaders = new HashSet<>();
      allowedHeaders.add("x-requested-with");
      allowedHeaders.add("Access-Control-Allow-Origin");
      allowedHeaders.add("origin");
      allowedHeaders.add("Content-Type");
      allowedHeaders.add("accept");
      allowedHeaders.add("X-PINGARUNER");

      router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(new HashSet<>(HttpMethod.values())).allowCredentials(true));

      SockJSHandlerOptions sockJSHandlerOptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
      SockJSHandler sockJSHandler = SockJSHandler.create(vertx, sockJSHandlerOptions);

      router.mountSubRouter("/myapp", sockJSHandler.socketHandler(sockJsSocket -> {
        logger.info("hehhsdsd");
        sockJsSocket.handler(sockJsSocket::write);
      }));

      vertx.createHttpServer().requestHandler(router).listen(8080)
        .onSuccess(server -> {
          logger.error("listen port: {}", server.actualPort());
        })
        .onFailure(t -> {
          logger.error(t.getMessage(), t);
        });
    }
  }

  public static class HttpVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
      vertx.createHttpServer()
        .requestHandler(req -> req.response().end("hello world"))
        .listen(8080)
        .onSuccess(server -> {
          logger.info(server.actualPort());
        })
        .onFailure(t -> {
          logger.error(t.getMessage(), t);
        });
    }
  }
}
