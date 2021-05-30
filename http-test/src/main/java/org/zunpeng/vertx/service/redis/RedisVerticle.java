package org.zunpeng.vertx.service.redis;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.core.ServerConstant;

public class RedisVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(RedisVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    logger.info("config of redisVerticle: {}", config().encode());
    RedisOptions redisOptions = new RedisOptions().setConnectionString(config().getString("connection"));
    Redis redis = Redis.createClient(vertx, redisOptions);
    RedisService.create(redis, ar -> {
      if (ar.failed()) {
        logger.error(ar.cause().getMessage(), ar.cause());
        startPromise.fail(ar.cause());
      } else {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder.setAddress(ServerConstant.EVENT_BUS_REDIS_ADDR).register(RedisService.class, ar.result());
        startPromise.complete();
      }
    });
  }
}
