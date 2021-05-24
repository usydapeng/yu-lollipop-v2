package org.zunpeng.vertx.service.redis;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(RedisVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    RedisOptions redisOptions = new RedisOptions().setConnectionString("redis://127.0.0.1:6379/1");
    Redis redis = Redis.createClient(vertx, redisOptions);
    RedisService.create(redis, ar -> {
      if (ar.failed()) {
        logger.error(ar.cause().getMessage(), ar.cause());
        startPromise.fail(ar.cause());
      } else {
        logger.info("redis service: {}", ar.result().toString());
        ServiceBinder binder = new ServiceBinder(vertx.getDelegate());
        binder.setAddress("redis_addr").register(RedisService.class, ar.result());
        startPromise.complete();
      }
    });
  }
}
