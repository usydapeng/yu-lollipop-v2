package org.zunpeng.vertx.service.mysql;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.templates.RowMapper;
import io.vertx.mutiny.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.Row;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.service.mysql.domain.Device;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MysqlServiceImpl implements MysqlService {

  private static final Logger logger = LogManager.getLogger(MysqlServiceImpl.class);

  private final Pool pool;

  public MysqlServiceImpl(Pool pool, Handler<AsyncResult<MysqlService>> handler) {
    this.pool = pool;
    SqlTemplate.forQuery(pool, "select 1")
      .mapTo(new RowMapper<JsonObject>(Row::toJson))
      .execute(new HashMap<>())
      .subscribe()
      .with(rowSet -> {
        logger.info("------- result of test: {}", rowSet.iterator().next());
        handler.handle(Future.succeededFuture(this));
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });


  }

  @Override
  public MysqlService getDeviceById(Long id, Handler<AsyncResult<Device>> handler) {
    String sql = "select * from device where id = #{id}";
    Map<String, Object> parameterMap = Collections.singletonMap("id", id);
    SqlTemplate.forQuery(pool, sql)
      .mapTo(Device.class)
      .execute(parameterMap)
      .subscribe()
      .with(rowSet -> {
        if (rowSet.size() == 0) {
          handler.handle(Future.succeededFuture(null));
          return;
        }
        if (rowSet.size() > 1) {
          handler.handle(Future.failedFuture(new RuntimeException("size of result is error")));
          return;
        }
        handler.handle(Future.succeededFuture(rowSet.iterator().next()));
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }
}
