package org.zunpeng.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.guice.injector.GuiceVerticleFactory;
import org.zunpeng.guice.injector.LollipopModule;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    logger.info("hello world");
    vertx.setPeriodic(1000, timerId -> {
      logger.info("time print: {}", timerId);
    });

    Injector injector = Guice.createInjector(new LollipopModule(vertx));
    vertx.registerVerticleFactory(new GuiceVerticleFactory(injector));

    ZhangService zhangService = injector.getInstance(ZhangService.class);
    LiService liService = injector.getInstance(LiService.class);
    logger.info("vertx: {}, {}", vertx.hashCode(), vertx);
    zhangService.println();
    liService.println();

    vertx.deployVerticle("guice:" + HelloVerticle.class.getName())
      .subscribe()
      .with(a -> {
        logger.info("result: {}", a);
      }, t -> {
        logger.error(t.getMessage(), t);
      });
  }
}
