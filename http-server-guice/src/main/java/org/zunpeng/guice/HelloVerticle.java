package org.zunpeng.guice;

import com.google.inject.Inject;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.guice.injector.MyHello;
import org.zunpeng.guice.injector.MyWorld;

public class HelloVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(HelloVerticle.class);

  private final String hello;
  private final String world;

  @Inject
  public HelloVerticle(@MyHello String hello, @MyWorld String world) {
    this.hello = hello;
    this.world = world;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    logger.info("hello: {}, world: {}", hello, world);
    super.start(startPromise);
  }
}
