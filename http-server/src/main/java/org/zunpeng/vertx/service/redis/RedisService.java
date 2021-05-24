package org.zunpeng.vertx.service.redis;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;

@ProxyGen
@VertxGen
public interface RedisService {

  @GenIgnore
  static RedisService create(Redis redis, Handler<AsyncResult<RedisService>> handler) {
    return new RedisServiceImpl(redis, handler);
  }

  @GenIgnore
  static org.zunpeng.vertx.service.redis.mutiny.RedisService createProxy(Vertx vertx, String address) {
    return new org.zunpeng.vertx.service.redis.mutiny.RedisService(new RedisServiceVertxEBProxy(vertx, address));
  }

  @Fluent
  RedisService get(String key, Handler<AsyncResult<String>> handler);

  @Fluent
  RedisService testUser(User other, Handler<AsyncResult<User>> handler);
}
