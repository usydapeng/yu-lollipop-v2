package org.zunpeng.guice.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.mutiny.core.Vertx;

public class LollipopModule extends AbstractModule {

  private final Vertx vertx;

  public LollipopModule(Vertx vertx) {
    this.vertx = vertx;
  }

  @Provides
  @Singleton
  public Vertx vertx() {
    return vertx;
  }

  @Provides
  @Singleton
  @MyHello
  public String hello() {
    return "hello";
  }

  @Provides
  @Singleton
  @MyWorld
  public String world() {
    return "world";
  }

}
