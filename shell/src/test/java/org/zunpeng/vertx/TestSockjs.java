package org.zunpeng.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class TestSockjs {

  private static final Logger logger = LogManager.getLogger(TestSockjs.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1));

    Router router = Router.router(vertx);

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");

    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(new HashSet<>(HttpMethod.values())).allowCredentials(true));

    SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);
    router.mountSubRouter("/socket.io", sockJSHandler.socketHandler(sockJsSocket -> {
      User user = sockJsSocket.webUser();
      Session session = sockJsSocket.webSession();
      logger.info("user: {}", user.attributes().encode());
      logger.info("session: {}, id: {}", session.data(), session.id());
      sockJsSocket.handler(buffer -> {
        logger.info("buffer: {}", buffer);
      });
    }));

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(9092)
      .onSuccess(server -> logger.info("port: {}", server.actualPort()))
      .onFailure(throwable -> logger.info(throwable.getMessage(), throwable));
  }
}
