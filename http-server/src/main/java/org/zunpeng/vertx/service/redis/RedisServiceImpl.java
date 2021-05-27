package org.zunpeng.vertx.service.redis;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.redis.client.ResponseType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.core.ListSubscriber;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class RedisServiceImpl implements RedisService {

  private static final Logger logger = LogManager.getLogger(RedisServiceImpl.class);

  private RedisAPI redisApi;

  public RedisServiceImpl(Redis redis, Handler<AsyncResult<RedisService>> handler) {
    redis.connect()
      .subscribe()
      .with(rd -> {
        this.redisApi = RedisAPI.api(rd);
        handler.handle(Future.succeededFuture(this));
      }, t -> {
        logger.error(t.getMessage(), t);
        handler.handle(Future.failedFuture(t));
      });
  }

  @Override
  public RedisService get(String key, Handler<AsyncResult<String>> handler) {
    this.redisApi.get(key)
      .subscribe()
      .with(response -> {
        if (response == null) {
          handler.handle(Future.succeededFuture(null));
        } else {
          logger.info("redis response type: {}, attr: {}, body: {}", response.type(), response.attributes(), response.toString(StandardCharsets.UTF_8));
          if (response.type() == ResponseType.BULK) {
            handler.handle(Future.succeededFuture(response.toString()));
          }
        }
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }

  @Override
  public RedisService list(String key, Handler<AsyncResult<List<String>>> handler) {
    this.redisApi.lrange(key, "0", "-1")
      .onItem()
      .transformToMulti(response -> {
        logger.info("redis response type: {}, attr: {}", response.type(), response.attributes());
        return response.toMulti();
      })
      .onItem()
      .transform(response -> {
        logger.info("redis response type: {}, attr: {}", response.type(), response.attributes());
        return response.toString();
      })
      .onFailure()
      .call((a) -> {
        logger.error(a.getMessage(), a);
        throw new RuntimeException(a);
      })
      .subscribe()
      .withSubscriber(new ListSubscriber<>(handler));
    return this;
  }

  @Override
  public RedisService testUser(User other, Handler<AsyncResult<User>> handler) {
    logger.info("user from request: {}", other.toJson().encode());
    Uni.createFrom().item(other).subscribe().withSubscriber(UniHelper.toSubscriber(handler));
    return this;
  }
}
