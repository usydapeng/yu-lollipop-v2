package org.zunpeng.provider;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import io.vertx.mutiny.servicediscovery.ServiceDiscovery;
import io.vertx.mutiny.servicediscovery.ServiceReference;
import io.vertx.mutiny.servicediscovery.types.MessageSource;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainVerticle2 extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MainVerticle2.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    EventBus eventBus = vertx.eventBus();
    MessageConsumer<String> consumer = eventBus.consumer("my-address");
    consumer.handler(message -> {
      logger.info("my-address: {}", message.body());
    });
    logger.info("init consumer");

    vertx.setTimer(1000, timerId -> {
      eventBus.publish("my-address", "hello");
    });

    logger.info("------------------- start -------------------");

    ServiceDiscoveryOptions serviceDiscoveryOptions =
      new ServiceDiscoveryOptions()
        .setAnnounceAddress("service-announce")
        .setName("my-name");
    ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx, serviceDiscoveryOptions);

    vertx.setTimer(1000, timerId -> {
      Record record = MessageSource.createRecord("some-message-source-service", "some-address");
      serviceDiscovery.publish(record)
        .subscribe()
        .with(publishedRecord -> {
          logger.info("publish record: {}", publishedRecord != null);
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });

      serviceDiscovery.getRecord(new JsonObject().put("name", "some-message-source-service"))
        .subscribe()
        .with(mRecord -> {
          ServiceReference serviceReference = serviceDiscovery.getReference(mRecord);
          MessageConsumer<String> messageConsumer = serviceReference.getAs(MessageConsumer.class);
          messageConsumer.handler(message -> {
            logger.info("{}: {}", message.address(), message.body());
          });
        }, throwable -> {
          logger.error(throwable.getMessage(), throwable);
        });
    });

    vertx.setTimer(3000, timerId -> {
      vertx.eventBus().publish("some-address", "zhangsan");
    });
  }
}
