package org.zunpeng.vertx.service.mysql;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import org.zunpeng.vertx.service.mysql.domain.Device;

@ProxyGen
@VertxGen
public interface MysqlService {

  @GenIgnore
  static MysqlService create(Pool pool, Handler<AsyncResult<MysqlService>> handler) {
    return new MysqlServiceImpl(pool, handler);
  }

  @GenIgnore
  static org.zunpeng.vertx.service.mysql.mutiny.MysqlService createProxy(Vertx vertx, String address) {
    return new org.zunpeng.vertx.service.mysql.mutiny.MysqlService(new MysqlServiceVertxEBProxy(vertx.getDelegate(), address));
  }

  @Fluent
  MysqlService getDeviceById(Long id, Handler<AsyncResult<Device>> handler);
}
