package org.zunpeng.vertx;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class Hello {

  private static final Logger logger = LogManager.getLogger(Hello.class);

  /**
   * console.log('1')
   *
   * setTimeout(() => {
   *   console.log('2')
   * }, 0)
   *
   * const p = new Promise((r) => {
   *   console.log('3')
   *   r()
   * })
   *
   * p.then(() => {
   *   console.log('4')
   * })
   *
   * console.log('5')
   */
  @Test
  public void demo(Vertx vertx, VertxTestContext testContext) {
    System.out.println("1");
    vertx.setTimer(100, id -> {
      System.out.println("2");
    });
    Uni<Integer> uni = Uni.createFrom().emitter(em -> {
      System.out.println("3");
      vertx.setTimer(100, idd -> {
        em.complete(1);
      });
    });
    uni.subscribe().with(a -> {
      System.out.println("4");
    });
    System.out.println("5");
  }
}
