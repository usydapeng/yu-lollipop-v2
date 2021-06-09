package org.zunpeng.vertx;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.junit5.VertxExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestSocketIO {

  private static final Logger logger = LogManager.getLogger(TestSocketIO.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1));
    Configuration configuration = new Configuration();
    configuration.setHostname("localhost");
    configuration.setPort(9092);
    configuration.setBossThreads(1);
    configuration.setWorkerThreads(1);
    configuration.setAuthorizationListener(new CustomAuthorizationListener());
    configuration.setExceptionListener(new CustomExceptionListener());

    SocketIOServer socketIOServer = new SocketIOServer(configuration);
    socketIOServer.addConnectListener(new CustomConnectListener());
    socketIOServer.addDisconnectListener(new CustomDisconnectListener());
    socketIOServer.addEventListener("chatevent", ChatObject.class, (client, data, ackRequest) -> {
      logger.info("chat info: " + data.getUserName() + " " + data.getMessage());
      Uni.createFrom()
        .emitter(em -> {
          logger.info("chat info emitter: " + data.getUserName() + " " + data.getMessage());
          em.complete("a");
        })
        .subscribe()
        .with(a -> {
          logger.info("chat info a: {}", a);
          socketIOServer.getBroadcastOperations().sendEvent("chatevent", data);
        });
    });
    socketIOServer.start();
    // vertx.setPeriodic(10000, timerId -> {
    //   logger.info("timerId: " + timerId);
    // });
    logger.info("end");
  }
}
