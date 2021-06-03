package org.zunpeng.vertx.service.mq;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.QueueOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;

public class RabbitMqInstance {

  private static final Logger logger = LogManager.getLogger(RabbitMqInstance.class);

  private final RabbitMQClient rabbitMQClient;
  private final Vertx vertx;

  public RabbitMqInstance(Vertx vertx, RabbitMQClient rabbitMqClient) {
    this.vertx = vertx;
    this.rabbitMQClient = rabbitMqClient;
  }

  public void listenQueue(String queue, BiConsumer<JsonObject, Handler<AsyncResult<Void>>> func) {
    rabbitMQClient.start()
      .onItem()
      .transformToUni(a -> {
        logger.info("rabbit mq client start success!");
        return rabbitMQClient.basicQos(1, false);
      })
      .onItem()
      .transformToUni(a -> {
        // create queue
        return rabbitMQClient.queueDeclare(queue, true, false, false, new JsonObject())
          .onItem()
          .transformToUni(declareOk -> {
            logger.info("queue: {}, consumerCount: {}, messageCount: {}", declareOk.getQueue(), declareOk.getConsumerCount(), declareOk.getMessageCount());
            return rabbitMQClient.queueBind(declareOk.getQueue(), "amq.direct", "");
          });
      })
      .onItem()
      .transformToUni(a -> {
        logger.info("rabbit mq client qos config success");
        QueueOptions queueOptions = new QueueOptions().setAutoAck(false).setKeepMostRecent(false);
        return rabbitMQClient.basicConsumer(queue, queueOptions);
      })
      .subscribe()
      .with(consumer -> {
        // 设置消费者的异常处理和正常处理
        consumer.exceptionHandler(throwable -> {
          logger.error("consumer exception: " + throwable.getMessage(), throwable);
        });
        consumer.handler(message -> {
          JsonObject messageBody = message.body().toJsonObject();
          logger.info("message body: {}", messageBody.encode());
          func.accept(messageBody, ar -> {
            if (ar.failed()) {
              logger.error(ar.cause().getMessage(), ar.cause());
              // nack message
              rabbitMQClient.basicNack(message.envelope().getDeliveryTag(), false, true)
                .subscribe()
                .with(a -> {}, t -> logger.error(t.getMessage(), t));
            } else {
              // ack message
              rabbitMQClient.basicAck(message.envelope().getDeliveryTag(), false)
                .subscribe()
                .with(a -> {}, t -> logger.error(t.getMessage(), t));
            }
          });
        });
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });
  }
}
