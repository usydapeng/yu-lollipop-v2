package org.zunpeng.vertx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class TestHello {

  private static final Logger logger = LogManager.getLogger(TestHello.class);

  @Test
  public void demo1() {
    logger.info("a a a a: {}", 1);
  }
}
