package org.zunpeng.vertx.service.mysql;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.core.ServerConstant;

public class MysqlVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MysqlVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    logger.info("mysql config: {}", config().encode());
    MySQLConnectOptions sqlConnectOptions = new MySQLConnectOptions(config());
    Pool pool = Pool.pool(vertx, sqlConnectOptions, new PoolOptions().setMaxSize(config().getInteger("maxSize", 4)));
    MysqlService.create(pool, ar -> {
      if (ar.failed()) {
        logger.error(ar.cause().getMessage(), ar.cause());
        startPromise.fail(ar.cause());
      } else {
        new ServiceBinder(vertx).setAddress(ServerConstant.EVENT_BUS_MYSQL_ADDR).register(MysqlService.class, ar.result());
        startPromise.complete();
      }
    });
  }
}
