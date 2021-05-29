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

import java.util.List;

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

  @Fluent
  MysqlService listDevice(Handler<AsyncResult<List<Device>>> handler);

  @Fluent
  MysqlService saveDevice(String sn, Handler<AsyncResult<Device>> handler);

  @Fluent
  MysqlService updateDevice(Long id, String sn, Handler<AsyncResult<Void>> handler);

  @Fluent
  MysqlService txUpdateDevice(Long id, String sn, Handler<AsyncResult<Void>> handler);
}
