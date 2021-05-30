package org.zunpeng.vertx.service.mysql;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.service.mysql.domain.Device;
import org.zunpeng.vertx.service.mysql.domain.DeviceRowMapper;

import java.util.*;

public class MysqlServiceImpl implements MysqlService {

  private static final Logger logger = LogManager.getLogger(MysqlServiceImpl.class);

  private final Pool pool;

  public MysqlServiceImpl(Pool pool, Handler<AsyncResult<MysqlService>> handler) {
    this.pool = pool;
    SqlTemplate.forQuery(pool, "select 1")
      .mapTo(Row::toJson)
      .execute(new HashMap<>())
      .onSuccess(rowSet -> {
        logger.info("------- result of test: {}", rowSet.iterator().next());
        handler.handle(Future.succeededFuture(this));
      })
      .onFailure(throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
  }

  @Override
  public MysqlService getDeviceById(Long id, Handler<AsyncResult<Device>> handler) {
    String sql = "select * from device where id = #{id}";
    Map<String, Object> parameterMap = Collections.singletonMap("id", id);
    SqlTemplate.forQuery(pool, sql)
      // TODO DeviceRowMapper.INSTANCE 不能是Device.class
      .mapTo(DeviceRowMapper.INSTANCE)
      .execute(parameterMap)
      .onSuccess(rowSet -> {
        if (rowSet.size() == 0) {
          handler.handle(Future.succeededFuture(null));
          return;
        }
        if (rowSet.size() > 1) {
          handler.handle(Future.failedFuture(new RuntimeException("size of result is error")));
          return;
        }
        handler.handle(Future.succeededFuture(rowSet.iterator().next()));
      })
      .onFailure(throwable -> {
        logger.error("exception: {}", throwable.getClass().getName());
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }

  @Override
  public MysqlService listDevice(Handler<AsyncResult<List<Device>>> handler) {
    String sql = "select * from device";
    SqlTemplate.forQuery(pool, sql)
      .mapTo(DeviceRowMapper.INSTANCE)
      .execute(new HashMap<>())
      .onSuccess(rowSet -> {
        List<Device> mobileDeviceList = new ArrayList<>();
        for (Device mobileDevice : rowSet) {
          mobileDeviceList.add(mobileDevice);
        }
        handler.handle(Future.succeededFuture(mobileDeviceList));
      })
      .onFailure(throwable -> {
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
      .onSuccess(rowSet -> {
        long lastInsertId = rowSet.property(MySQLClient.LAST_INSERTED_ID);
        logger.info("lastInsertId: {}, {} row(s) updated!", lastInsertId, rowSet.rowCount());
        handler.handle(Future.succeededFuture(new Device(lastInsertId, sn)));
      })
      .onFailure(throwable -> {
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
      .onSuccess(rowSet -> {
        logger.info("{} row(s) updated!", rowSet.rowCount());
        handler.handle(Future.succeededFuture());
      })
      .onFailure(throwable -> {
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
    pool.withTransaction(conn -> SqlTemplate.forUpdate(conn, sql).execute(paramMap))
      .onSuccess(rowSet -> {
        logger.info("{} row(s) updated!", rowSet.rowCount());
        handler.handle(Future.succeededFuture());
      })
      .onFailure(throwable -> {
        logger.error(throwable.getMessage(), throwable);
        handler.handle(Future.failedFuture(throwable));
      });
    return this;
  }
}
