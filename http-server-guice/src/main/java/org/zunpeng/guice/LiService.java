package org.zunpeng.guice;

import com.google.inject.Inject;
import io.vertx.mutiny.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LiService {

  private static final Logger logger = LogManager.getLogger(LiService.class);

  private final Vertx vertx;
  private final ZhangService zhangService;

  @Inject
  public LiService(Vertx vertx, ZhangService zhangService) {
    this.vertx = vertx;
    this.zhangService = zhangService;
  }

  public void println() {
    logger.info("----------------: {}", vertx != null);
    zhangService.println();
    logger.info("li vertx: {}, {}", vertx.hashCode(), vertx);
  }
}
