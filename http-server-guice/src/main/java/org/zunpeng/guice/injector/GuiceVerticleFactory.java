package org.zunpeng.guice.injector;

import com.google.inject.Injector;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.Callable;

public class GuiceVerticleFactory implements VerticleFactory {

  private static final Logger logger = LogManager.getLogger(GuiceVerticleFactory.class);

  private static final String GUICE_PREFIX = "guice";

  private final Injector injector;

  public GuiceVerticleFactory(Injector injector) {
    Objects.requireNonNull(injector);
    this.injector = injector;
  }

  @Override
  public String prefix() {
    return GUICE_PREFIX;
  }

  @Override
  public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
    try {
      Verticle verticle = (Verticle) injector.getInstance(classLoader.loadClass(VerticleFactory.removePrefix(verticleName)));
      promise.complete(() -> verticle);
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
    }
  }
}
