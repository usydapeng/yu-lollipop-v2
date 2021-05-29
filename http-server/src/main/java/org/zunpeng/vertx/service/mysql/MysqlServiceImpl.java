package org.zunpeng.vertx.service.mysql;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mysqlclient.MySQLClient;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.templates.RowMapper;
import io.vertx.mutiny.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.Row;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.service.mysql.domain.Device;

import java.util.*;

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

  @Override
  public MysqlService listDevice(Handler<AsyncResult<List<Device>>> handler) {
    String sql = "select * from device";
    SqlTemplate.forQuery(pool, sql)
      .mapTo(Device.class)
      .execute(new HashMap<>())
      .subscribe()
      .with(rowSet -> {
        List<Device> deviceList = new ArrayList<>();
        for (Device device : rowSet) {
          deviceList.add(device);
        }
        handler.handle(Future.succeededFuture(deviceList));
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }

  @Override
  public MysqlService saveDevice(String sn, Handler<AsyncResult<Device>> handler) {
    String sql = "insert into device(sn) values (#{sn})";
    SqlTemplate.forUpdate(pool, sql)
      .execute(Collections.singletonMap("sn", sn))
      .subscribe()
      .with(rowSet -> {
        long lastInsertId = rowSet.property(MySQLClient.LAST_INSERTED_ID);
        logger.info("lastInsertId: {}, {} row(s) updated!", lastInsertId, rowSet.rowCount());
        handler.handle(Future.succeededFuture(new Device(lastInsertId, sn)));
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }

  @Override
  public MysqlService updateDevice(Long id, String sn, Handler<AsyncResult<Void>> handler) {
    String sql = "update device set sn = #{sn} where id = #{id}";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", id);
    paramMap.put("sn", sn);
    SqlTemplate.forUpdate(pool, sql)
      .execute(paramMap)
      .subscribe()
      .with(rowSet -> {
        logger.info("{} row(s) updated!", rowSet.rowCount());
        handler.handle(Future.succeededFuture());
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }

  @Override
  public MysqlService txUpdateDevice(Long id, String sn, Handler<AsyncResult<Void>> handler) {
    String sql = "update device set sn = #{sn} where id = #{id}";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", id);
    paramMap.put("sn", sn);
    pool.withTransaction(conn ->
      SqlTemplate.forUpdate(conn, sql)
        .execute(paramMap)
        // 模拟失败，触发回滚
        // .onItem()
        // .failWith(a -> {
        //   logger.info("{} row(s) updated!", a.rowCount());
        //   return new RuntimeException("error");
        // })
    )
      .subscribe()
      .with(a -> {
        logger.info("{} row(s) updated!", a.rowCount());
        handler.handle(Future.succeededFuture());
      }, t -> {
        logger.error(t.getMessage(), t);
        handler.handle(Future.failedFuture(t));
      });
    return this;
  }
}
