package org.zunpeng.provider;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.servicediscovery.ServiceDiscovery;
import io.vertx.mutiny.servicediscovery.ServiceReference;
import io.vertx.mutiny.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    logger.info("------------------- start -------------------");

    ServiceDiscoveryOptions serviceDiscoveryOptions =
      new ServiceDiscoveryOptions()
        .setAnnounceAddress("service-announce")
        .setName("my-name");
    ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx, serviceDiscoveryOptions);

    Record record1 = new Record()
      .setType("event-service-proxy")
      .setLocation(new JsonObject().put("endpoint", "the-service-address"))
      .setName("my-service")
      .setMetadata(new JsonObject().put("some-label", "some-value"));

    serviceDiscovery.publish(record1)
      .subscribe()
      .with(publishedRecord -> {
        logger.info("publish record 1");
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });

    Record record2 = HttpEndpoint.createRecord("som-rest-api", "localhost", 8080, "/api");
    serviceDiscovery.publish(record2)
      .subscribe()
      .with(publishedRecord -> {
        logger.info("publish record 2");
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });

    vertx.setTimer(1000, timerId -> {
      logger.info("------------------- getRecord -------------------");
      serviceDiscovery.getRecord(r -> true)
        .subscribe()
        .with(getRecord -> {
          logger.info("getRecord1: {}", getRecord == null);
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });

      serviceDiscovery.getRecord((JsonObject) null)
        .subscribe()
        .with(getRecord -> {
          logger.info("getRecord2: {}", getRecord == null);
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });

      serviceDiscovery.getRecords((JsonObject) null)
        .subscribe()
        .with(records -> {
          logger.info("size of records2: {}", (records == null ? 0 : records.size()));
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });

      serviceDiscovery.getRecords(new JsonObject().put("some-label", "some-value"))
        .subscribe()
        .with(records -> {
          logger.info("size of records2: {}", (records == null ? 0 : records.size()));
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });


      serviceDiscovery.getRecords(new JsonObject().put("some-label", "some-value"))
        .subscribe()
        .with(records -> {
          logger.info("size of records3: {}", (records == null ? 0 : records.size()));

          ServiceReference reference1 = serviceDiscovery.getReference(records.get(0));
          HttpClient httpClient = reference1.getAs(HttpClient.class);
          reference1.release();
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });
    });

    startPromise.complete();
  }
}
