package org.zunpeng.provider;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.servicediscovery.ServiceDiscovery;
import io.vertx.mutiny.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Provider {

  private static final Logger logger = LogManager.getLogger(Provider.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(4).setEventLoopPoolSize(4).setInternalBlockingPoolSize(4));
    provide1(vertx);
    provide2(vertx);
    consumer1(vertx);
    consumer2(vertx);
  }

  public static void provide1(Vertx vertx) {
    ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions().setAnnounceAddress("service-announce").setName("my-service");
    ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx, serviceDiscoveryOptions);
    vertx.createHttpServer()
      .requestHandler(req -> req.response().endAndForget("server one"))
      .listen(9081)
      .onItem()
      .transformToUni(server -> {
        Record record = HttpEndpoint.createRecord("hello", false, "127.0.0.1", server.actualPort(), "/",
          new JsonObject().put("address", "120"));
        return serviceDiscovery.publish(record);
      })
      .subscribe()
      .with(publishedRecord -> {
        logger.info("publish record 1: {}", publishedRecord != null);
      }, throwable -> {
        logger.info(throwable.getMessage(), throwable);
      });
  }

  public static void provide2(Vertx vertx) {
    ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions().setAnnounceAddress("service-announce").setName("my-service");
    ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx, serviceDiscoveryOptions);
    vertx.createHttpServer()
      .requestHandler(req -> req.response().endAndForget("server two"))
      .listen(9082)
      .onItem()
      .transformToUni(server -> {
        Record record = HttpEndpoint.createRecord("hello", false, "127.0.0.1", server.actualPort(), "/",
          new JsonObject().put("address", "120"));
        return serviceDiscovery.publish(record);
      })
      .subscribe()
      .with(publishedRecord -> {
        logger.info("publish record 2: {}", publishedRecord != null);
      }, throwable -> {
        logger.info(throwable.getMessage(), throwable);
      });
  }

  public static void consumer1(Vertx vertx) {
    vertx.setTimer(2000, timerId -> {
      ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions().setAnnounceAddress("service-announce").setName("my-service");
      ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx, serviceDiscoveryOptions);
      serviceDiscovery.getRecords(new JsonObject().put("name", "hello"))
        .subscribe()
        .with(records -> {
          logger.info("size of records: {}", records.size());
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });
    });
  }

  public static void consumer2(Vertx vertx) {
    ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions().setAnnounceAddress("service-announce").setName("my-service");
    ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx, serviceDiscoveryOptions);

    vertx.setPeriodic(1000, timerId -> {
      HttpEndpoint.getClient(serviceDiscovery, new JsonObject().put("name", "hello"))
        .onItem()
        .transformToUni(httpClient -> {
          // logger.info("get httpClient: {}", httpClient != null);
          return httpClient.request(HttpMethod.GET, "/");
        })
        .onItem()
        .transformToUni(req -> {
          // logger.info("req: {}", req != null);
          try {
            return req.send();
          } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw t;
          }
        })
        .subscribe()
        .with(resp -> {
          resp.body()
            .subscribe()
            .with(buffer -> {
              logger.info("resp body: {}", buffer);
            }, throwable -> {
              logger.error(throwable.getMessage(), throwable);
            });
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });
    });
  }
}
