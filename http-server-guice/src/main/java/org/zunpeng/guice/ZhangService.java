package org.zunpeng.guice;

import com.google.inject.Inject;
import io.vertx.mutiny.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZhangService {

  private static final Logger logger = LogManager.getLogger(ZhangService.class);

  private final Vertx vertx;

  @Inject
  public ZhangService(Vertx vertx) {
    this.vertx = vertx;
  }

  public void println() {
    logger.info("vertx: {}, {}", vertx.hashCode(), vertx);
  }
}
