package org.zunpeng.vertx;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.tuples.Tuple;
import io.smallrye.mutiny.tuples.Tuple3;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@ExtendWith(VertxExtension.class)
public class TestMutiny {

  private static final Logger logger = LogManager.getLogger(TestMutiny.class);

  private final Executor executor = new ForkJoinPool(1);

  @Test
  public void demo1() {
    Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5)
      .emitOn(executor)
      .onItem()
      .transform(i -> {
        System.out.println(Thread.currentThread() + " : is working on item : " + i);
        return i * 2;
      });
    multi.subscribe().with(this::printWithThread);
  }

  @Test
  public void demo2() throws Exception {
    printWithThread("start");
    Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(2));
    ticks.subscribe().with(this::printWithThread);
    Thread.sleep(10000);
  }

  @Test
  public void demo3() throws Exception {
    System.out.println("----------: " + LocalDateTime.now());
    Uni.createFrom().item("hello world")
      .emitOn(executor)
      .onItem().delayIt().by(Duration.ofSeconds(2))
      .onItem().transform(s -> s + ": 张三 : " + LocalDateTime.now())
      .subscribe().with(this::printWithThread);
    Thread.sleep(10000);
  }

  @Test
  public void demo4() throws Exception {
    Multi.createFrom().items("a", "b", "c")
      .onItem().failWith((Supplier<Throwable>) Exception::new)
      .onFailure().invoke(() -> System.out.println("------: " + LocalDateTime.now()))
      .onFailure().retry().atMost(1)
      // .onFailure().recoverWithItem("feedbackItem")
      .subscribe().with(System.out::println, this::printWithThread);
  }

  @RepeatedTest(100000)
  public void demo5() throws Exception {
    System.out.println(LocalDateTime.now() + " getSyn: start ==========================");
    InetAddress[] allByName = InetAddress.getAllByName("api.rzrtc.com");
    for (InetAddress ip : allByName) {
      System.out.println(LocalDateTime.now() + " getSyn: Address:" + ip.getHostAddress() + " Name:" + ip.getHostName());
    }
  }

  @Test
  public void demo6() throws Exception {
    Uni<String> uni = Uni.createFrom().item("aa");
    Uni<String> u = uni.onItem().invoke(i -> System.out.println("Received item: " + i));
    u.subscribe().with(a -> {
      System.out.println("hello world");
      logger.info("-------: {}", a);
    });
    Thread.sleep(1000);
  }

  @Test
  public void demo7() throws Exception {
    Multi.createFrom().items("a", "b", "c", "d")
      .onItem()
      .transform(i -> {
        if (i.equals("d")) {
          throw new RuntimeException("runtime exception");
        } else {
          return i.toUpperCase(Locale.ROOT);
        }
      })
      .subscribe().with(logger::info, error -> logger.error(error.getMessage(), error));
  }

  @Test
  public void demo8() throws Exception {
    Multi.createFrom().items("a", "b", "c", "d", "e")
      .onItem()
      .transform(i -> {
        try {
          Thread.sleep(new Random().nextInt(2000));
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
        return i.toUpperCase(Locale.ROOT);
      })
      .subscribe().with(logger::info);
  }

  @Test
  public void demo8_1() throws Exception {
    Multi.createFrom().items("a", "b", "c", "d", "e")
      .onItem()
      .transform(Unchecked.function(i -> {
        Thread.sleep(new Random().nextInt(2000));
        return i.toUpperCase(Locale.ROOT);
      }))
      .subscribe().with(logger::info);
  }

  @Test
  public void demo9() throws Exception {
    Multi.createFrom().items("a", "b", "c", "d", "e")
      .onItem()
      .transformToUni(i ->
        Uni.createFrom().emitter(em -> {
          try {
            Thread.sleep(new Random().nextInt(2000));
            em.complete(i.toUpperCase(Locale.ROOT));
          } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
          }
        }))
      .merge()
      // .concatenate()
      .subscribe().with(logger::info, t -> logger.error(t.getMessage(), t));
  }

  /**
   * merge 和 concatenate 都是顺序返回，原因不明
   *
   * @throws Exception
   */
  @Test
  public void demo10() throws Exception {
    Multi.createFrom().items("a", "b", "c", "d", "e")
      .onItem()
      .transformToUniAndMerge(i ->
        Uni.createFrom().emitter(em -> {
          try {
            Thread.sleep(new Random().nextInt(2000));
            em.complete(i.toUpperCase(Locale.ROOT));
          } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
          }
        }))
      .subscribe().with(logger::info, t -> logger.error(t.getMessage(), t));
  }

  @Test
  public void demo10_fix(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items("a", "b", "c", "d", "e")
      .onItem()
      .transformToUniAndMerge(i ->
        Uni.createFrom().emitter(em -> {
          vertx.setTimer(new Random().nextInt(1000), timerId -> {
            em.complete(i.toUpperCase(Locale.ROOT));
          });
        }))
      .subscribe()
      .with(logger::info, t -> logger.error(t.getMessage(), t));
  }

  /**
   * invoke 同步的
   *
   * @throws Exception
   */
  @Test
  public void demo11() throws Exception {
    Uni.createFrom().item(1)
      .onItem()
      .transformToUni(i -> {
        if (i == 1) {
          throw new RuntimeException("error");
        }
        return Uni.createFrom().item(i * 10);
      })
      .onFailure().invoke(t -> logger.error("--- 1 ---------------- " + t.getMessage(), t))
      .subscribe().with(logger::info, t -> logger.error("--- 2 ---------------- " + t.getMessage(), t));
  }

  /**
   * call 异步的，还是先返回1，再返回2
   *
   * @throws Exception
   */
  @Test
  public void demo12() throws Exception {
    Uni.createFrom().item(1)
      .onItem()
      .transformToUni(i -> {
        if (i == 1) {
          throw new RuntimeException("error");
        }
        return Uni.createFrom().item(i * 10);
      })
      .onFailure()
      .call(t -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        logger.error("--- 1 ---------------- " + t.getMessage(), t);
        return Uni.createFrom().nullItem();
      })
      .subscribe().with(logger::info, t -> logger.error("--- 2 ---------------- " + t.getMessage(), t));
  }

  @Test
  public void demo12_fix(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item(1)
      .onItem()
      .transformToUni(i -> {
        if (i == 1) {
          throw new RuntimeException("error");
        }
        return Uni.createFrom().item(i * 10);
      })
      .onFailure()
      .call(t ->
        Uni.createFrom()
          .emitter(em -> {
            vertx.setTimer(1000, timerId -> {
              logger.error("--- 1 ---------------- " + t.getMessage(), t);
              em.complete(null);
            });
          }))
      .subscribe()
      .with(result -> {
        logger.info(result);
        testContext.completeNow();
      }, t -> {
        logger.error("--- 2 ---------------- " + t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  /**
   * result: 一百
   */
  @Test
  public void demo13() {
    Uni.createFrom().emitter(em -> {
      if (1 < 2) {
        throw new RuntimeException("err");
      } else {
        em.complete(100);
      }
    })
      .onFailure().recoverWithItem("一百")
      .subscribe().with(logger::info);
  }

  // 失败重试
  @Test
  public void demo14() throws Exception {
    Uni.createFrom().emitter(em -> {
      if (1 < 2) {
        throw new RuntimeException("err");
      } else {
        em.complete(100);
      }
    }).onFailure().invoke(t -> logger.error(t.getMessage(), t))
      .onFailure().retry()
      .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(3))
      .atMost(3)
      .subscribe().with(logger::info, t -> logger.error("=====" + t.getMessage(), t));

    Thread.sleep(1000 * 60);
  }

  @Test
  public void demo15(Vertx vertx, VertxTestContext testContext) throws Exception {

    Uni<String> uniA = Uni.createFrom()
      .emitter(em -> {
        logger.info("A: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("A");
        });
      });
    Uni<String> uniB = Uni.createFrom()
      .emitter(em -> {
        logger.info("B: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("B");
        });
      });
    Uni<String> uniC = Uni.createFrom()
      .emitter(em -> {
        logger.info("C: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("C");
        });
      });
    Uni.combine().all().unis(uniA, uniB, uniC).asTuple()
      .subscribe()
      .with(a -> {
        logger.info("a: " + a.getItem1() + " b: " + a.getItem2() + " c: " + a.getItem3());
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo15_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni<String> uniA = Uni.createFrom()
      .emitter(em -> {
        logger.info("A: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("A");
        });
      });
    Uni<String> uniB = Uni.createFrom()
      .emitter(em -> {
        logger.info("B: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("B");
        });
      });
    Uni<String> uniC = Uni.createFrom()
      .emitter(em -> {
        logger.info("C: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("C");
        });
      });
    Uni.combine().all().unis(uniA, uniB, uniC).asTuple()
      .subscribe()
      .with(a -> {
        logger.info("a: " + a.getItem1() + " b: " + a.getItem2() + " c: " + a.getItem3());
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo15_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni<String> uniA = Uni.createFrom()
      .emitter(em -> {
        logger.info("A: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("A");
        });
      });
    Uni<String> uniB = Uni.createFrom()
      .emitter(em -> {
        logger.info("B: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("B");
        });
      });
    Uni<String> uniC = Uni.createFrom()
      .emitter(em -> {
        logger.info("C: ");
        vertx.setTimer(new Random().nextInt(2000), timerId -> {
          em.complete("C");
        });
      });
    Uni.combine().all().unis(uniA, uniB, uniC)
      .combinedWith(listOfResult -> {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("A", listOfResult.get(0));
        map.put("B", listOfResult.get(1));
        map.put("C", listOfResult.get(2));
        return map;
      }).subscribe()
      .with(a -> {
        logger.info("a: " + a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo16(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi<String> multiA = Multi.createFrom().emitter(em -> vertx.setTimer(new Random().nextInt(2000), timerId -> {
      em.emit("1");
      em.emit("2");
      em.emit("3");
      em.emit("4");
      em.complete();
    }));
    Multi<String> multiB = Multi.createFrom().emitter(em -> vertx.setTimer(new Random().nextInt(2000), timerId -> {
      em.emit("a");
      em.emit("b");
      em.emit("c");
      em.emit("d");
      em.emit("e");
      em.complete();
    }));
    Multi.createBy().combining().streams(multiA, multiB).asTuple()
      .subscribe()
      .with(a -> {
        logger.info(a.asList());
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo16_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi<String> multiA = Multi.createFrom().emitter(em -> vertx.setTimer(new Random().nextInt(2000), timerId -> {
      em.emit("1");
      em.emit("2");
      em.emit("3");
      em.emit("4");
      em.complete();
    }));
    Multi<String> multiB = Multi.createFrom().emitter(em -> vertx.setTimer(new Random().nextInt(2000), timerId -> {
      em.emit("a");
      em.emit("b");
      em.emit("c");
      em.emit("d");
      em.emit("e");
      em.complete();
    }));
    Multi.createBy().combining().streams(multiA, multiB).using(list -> list)
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo16_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("--------------");
    Multi<String> multiA = Multi.createFrom().emitter(em -> {
      vertx.setTimer(500, id -> em.emit("1"));
      vertx.setTimer(600, id -> em.emit("2"));
      vertx.setTimer(700, id -> em.emit("3"));
      vertx.setTimer(800, id -> em.emit("4"));
      vertx.setTimer(8000, id -> em.complete());
    });
    Multi<String> multiB = Multi.createFrom().emitter(em -> {
      vertx.setTimer(500, id -> em.emit("a"));
      vertx.setTimer(600, id -> em.emit("b"));
      vertx.setTimer(700, id -> em.emit("c"));
      vertx.setTimer(800, id -> em.emit("d"));
      vertx.setTimer(8000, id -> em.complete());
    });
    Multi.createBy().combining().streams(multiA, multiB).latestItems().using(list -> list)
      .subscribe()
      .with(logger::info,
        t -> {
          logger.error(t.getMessage(), t);
          testContext.failNow(t);
        }, testContext::completeNow);
  }

  @Test
  public void demo16_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("--------------");
    Multi<String> multiA = Multi.createFrom().emitter(em -> {
      vertx.setTimer(500, id -> em.emit("1"));
      vertx.setTimer(600, id -> em.emit("2"));
      vertx.setTimer(700, id -> em.emit("3"));
      vertx.setTimer(800, id -> em.emit("4"));
      vertx.setTimer(8000, id -> em.complete());
    });
    Multi<String> multiB = Multi.createFrom().emitter(em -> {
      vertx.setTimer(500, id -> em.emit("a"));
      vertx.setTimer(600, id -> em.emit("b"));
      vertx.setTimer(700, id -> em.emit("c"));
      vertx.setTimer(800, id -> em.emit("d"));
      vertx.setTimer(8000, id -> em.complete());
    });
    Multi.createBy().combining().streams(multiA, multiB).latestItems().asTuple()
      .subscribe()
      .with(a -> logger.info(a.asList()),
        t -> {
          logger.error(t.getMessage(), t);
          testContext.failNow(t);
        }, testContext::completeNow);
  }

  // where 同步
  @Test
  public void demo17(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8)
      .select().where(i -> i > 3)
      .collect().asList().subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  // when 是异步的
  @Test
  public void demo17_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8)
      .select().when(i -> Uni.createFrom().item(i > 3))
      .collect().asList().subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo18(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9)
      .select().first()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo18_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9)
      .select().first(3)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo18_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9)
      .select().first(i -> i > 1)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo18_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(10, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      .select().first(i -> i > 1)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo18_4(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().emitter(em -> {
      vertx.setTimer(1100, timerId -> {
        logger.info("em: a");
        em.emit("a");
      });
      vertx.setTimer(1200, timerId -> {
        logger.info("em: b");
        em.emit("b");
      });
      vertx.setTimer(1500, timerId -> {
        logger.info("em: c");
        em.emit("c");
      });
      vertx.setTimer(2000, timerId -> {
        logger.info("em: complete");
        em.complete();
      });
    }).select()
      .first(Duration.ofMillis(1200))
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo18_5(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(10, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      .skip().first(3)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo19(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(10, 10, 11, 1, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      .select().distinct()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo18_6(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(10, 11, 1, 0, 3, 4, 5, 6, 7, 8, 9)
      .skip().first(i -> i > 1)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo19_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(10, 10, 11, 1, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      .skip().repetitions()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo20(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> em.complete(null))
      .onItem().ifNull().continueWith("hello")
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo20_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> em.complete(null))
      .onItem().ifNull().switchTo(() -> Uni.createFrom().item("hello"))
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo20_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> em.complete(null))
      .onItem().ifNull().failWith(() -> new Exception("Boom"))
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo20_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item("String")
      .onItem().ifNull().continueWith("hello")
      .onItem().ifNotNull().transform(String::toUpperCase)
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo21(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> {
      em.complete(10);
    }).ifNoItem().after(Duration.ofSeconds(1))
      .recoverWithItem("some fallback item")
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo21_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> {
      vertx.setTimer(1001, timerId -> em.complete(10));
    }).ifNoItem().after(Duration.ofSeconds(1))
      .recoverWithItem("some fallback item")
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo21_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> {
      vertx.setTimer(1001, timerId -> em.complete(10));
    }).ifNoItem().after(Duration.ofSeconds(1)).fail()
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo21_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> {
      vertx.setTimer(1001, timerId -> em.complete(10));
    }).ifNoItem().after(Duration.ofSeconds(1)).fail()
      .onFailure(TimeoutException.class).recoverWithItem("we got a failure")
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo22(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item("10")
      .onItem().delayIt().by(Duration.ofMillis(1001))
      .ifNoItem().after(Duration.ofSeconds(1)).fail()
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo22_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item("10")
      .onItem().delayIt()
      .until(a -> {
        logger.info("------: {}", a);
        return Uni.createFrom().item(1100);
      })
      .ifNoItem().after(Duration.ofSeconds(1)).fail()
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo23(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items(1, 2, 3, 4, 5)
      .onItem()
      .call(i -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(new Random().nextInt(2000))))
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo24(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onOverflow().drop()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  /**
   * https://smallrye.io/smallrye-mutiny/guides
   *
   * @param vertx
   * @param testContext
   * @throws Exception
   */
  @Test
  public void demo25(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("hello world");
    Executor executor = Executors.newSingleThreadExecutor();
    PollableDataSource source = new PollableDataSource();
    Uni.createFrom().item(source::poll).runSubscriptionOn(executor)
      .repeat()
      .atMost(3)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo26(Vertx vertx, VertxTestContext testContext) throws Exception {
    Infrastructure.setOperatorLogger((id, event, value, err) -> {
      final Logger innerLogger = LogManager.getLogger(id);
      if (err != null) {
        innerLogger.error(event + "(" + err.getClass() + "(" + err.getMessage() + "))", err);
      } else {
        if (value != null) {
          innerLogger.info(event + "(" + value + ")");
        } else {
          innerLogger.info(event + "()");
        }
      }
    });
    Multi.createFrom().items(1, 2, 3, 4, 5)
      .onItem()
      .call(i -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(new Random().nextInt(2000))))
      .log("hello-world-log")
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo27(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createBy().repeating()
      .completionStage(
        AtomicInteger::new,
        state -> {
          logger.info("state: {}", state.get());
          return page(state.getAndIncrement());
        })
      .until(List::isEmpty)
      .onItem().disjoint()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  // 分页
  @Test
  public void demo27_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createBy().repeating()
      .uni(
        AtomicInteger::new,
        state -> {
          logger.info("state: {}", state.get());
          return pageUni(state.getAndIncrement());
        })
      .until(List::isEmpty)
      .onItem().disjoint()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  // whilst 和 until 行为刚好相反
  @Test
  public void demo27_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createBy().repeating()
      .completionStage(
        AtomicInteger::new,
        state -> {
          logger.info("state: {}", state.get());
          return page(state.getAndIncrement());
        })
      .whilst(list -> !list.isEmpty())
      .onItem().disjoint()
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  /**
   * Except indicated otherwise, Mutiny invokes the next stage using the thread emitting the event from upstream.
   */
  @Test
  public void demo28(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().emitter(em -> vertx.setTimer(1000, timerId -> em.complete("hello")))
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo28_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("hello world start");
    Uni.createFrom().emitter(em -> {
      logger.info("hello world");
      em.complete("hello");
    })
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo28_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("hello world start");
    Uni.createFrom().emitter(em -> {
      logger.info("hello world");
      em.complete("hello");
    })
      .emitOn(Executors.newSingleThreadExecutor())
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo28_4(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("hello world start");
    Object aa = Uni.createFrom().emitter(em -> {
      logger.info("hello world");
      em.complete("hello");
    })
      .emitOn(Executors.newSingleThreadExecutor())
      .onItem()
      .invoke(s -> logger.info("receive item: " + s + " on thread"))
      .await().indefinitely();
    logger.info(aa);
  }

  @Test
  public void demo29(Vertx vertx, VertxTestContext testContext) throws Exception {
    Executor executor = Executors.newSingleThreadExecutor();
    logger.info("------- start");
    Multi.createFrom().items(1, 2, 3, 4, 5)
      .emitOn(executor)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo29_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Executor executor = Executors.newSingleThreadExecutor();
    logger.info("------- start");
    Multi.createFrom().items(1, 2, 3, 4, 5)
      .runSubscriptionOn(executor)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo30(Vertx vertx, VertxTestContext testContext) throws Exception {
    try {
      String helloStr = Uni.createFrom().item("hello world")
        .onItem().delayIt().by(Duration.ofMillis(1010))
        .await().indefinitely();
      logger.info(helloStr);
      testContext.completeNow();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      testContext.failNow(t);
    }
  }

  @Test
  public void demo30_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    try {
      String helloStr = Uni.createFrom().item("hello world")
        .onItem().delayIt().by(Duration.ofMillis(1010))
        .await().atMost(Duration.ofSeconds(1));
      logger.info(helloStr);
      testContext.completeNow();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      testContext.failNow(t);
    }
  }

  @Test
  public void demo30_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    try {
      Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7)
        .subscribe().asIterable().forEach(i -> {
        logger.info("i: {}", i);
      });
      testContext.completeNow();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      testContext.failNow(t);
    }
  }

  @Test
  public void demo31(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("thread: {}", Infrastructure.getDefaultWorkerPool());
    logger.info("hello");
    testContext.completeNow();
  }

  @Test
  public void demo31_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item(1)
      .runSubscriptionOn(Infrastructure.getDefaultExecutor())
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo31_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item(1)
      // .runSubscriptionOn(Infrastructure.getDefaultExecutor())
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  // 阻塞
  @Test
  public void demo31_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item(() -> {
      try {
        Thread.sleep(1000);
        return "a";
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
        throw new RuntimeException(t);
      }
    }).subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
    logger.info("-----a - ---");
  }

  // 非阻塞
  @Test
  public void demo31_4(Vertx vertx, VertxTestContext testContext) throws Exception {
    Executor executor = Executors.newSingleThreadExecutor();
    Uni.createFrom().item(() -> {
      try {
        Thread.sleep(1000);
        return "a";
      } catch (Throwable t) {
        logger.error(t.getMessage(), t);
        throw new RuntimeException(t);
      }
    }).runSubscriptionOn(executor)
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
    logger.info("-----a - ---");
  }

  @Test
  public void demo32(Vertx vertx, VertxTestContext testContext) throws Exception {
    AtomicInteger atomicInteger = new AtomicInteger();
    Uni<Object> uni = Uni.createFrom().emitter(em -> em.complete(atomicInteger.getAndIncrement()))
      .memoize().indefinitely();
    logger.info(uni.await().indefinitely());
    logger.info(uni.await().indefinitely());
    testContext.completeNow();
  }

  @Test
  public void demo32_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    AtomicInteger atomicInteger = new AtomicInteger();
    Uni<Object> uni = Uni.createFrom().emitter(em -> em.complete(atomicInteger.getAndIncrement()));
    logger.info(uni.await().indefinitely());
    logger.info(uni.await().indefinitely());
    testContext.completeNow();
  }

  @Test
  public void demo32_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    AtomicInteger atomicInteger = new AtomicInteger();
    Uni<Integer> uni = Uni.createFrom().emitter(em -> {
      logger.info("i: {}", atomicInteger.get());
      em.complete(atomicInteger.getAndIncrement());
    });
    CompletionStage<Integer> completionStage = uni.subscribeAsCompletionStage();
    CompletionStage<Integer> completionStage1 = uni.subscribeAsCompletionStage();
    completionStage1.whenComplete((a, t) -> {
      if (t != null) {
        logger.error(t.getMessage(), t);
      } else {
        logger.info(a);
      }
    });
    completionStage.whenComplete((a, t) -> {
      if (t != null) {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      } else {
        logger.info(a);
        testContext.completeNow();
      }
    });
  }

  @Test
  public void demo33(Vertx vertx, VertxTestContext testContext) throws Exception {
    Executor executor = Executors.newSingleThreadExecutor();
    Uni.createFrom().completionStage(
      CompletableFuture.supplyAsync(() -> "hello", executor)
    ).onItem().transform(String::toUpperCase)
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  // recommended
  @Test
  public void demo33_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Executor executor = Executors.newSingleThreadExecutor();
    Uni.createFrom().completionStage(
      () -> CompletableFuture.supplyAsync(() -> "hello", executor)
    ).onItem().transform(String::toUpperCase)
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  // recommended
  @Test
  public void demo33_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Executor executor = Executors.newSingleThreadExecutor();
    Multi.createFrom().completionStage(
      () -> CompletableFuture.supplyAsync(() -> "hello", executor)
    ).onItem().transform(String::toUpperCase)
      .subscribe()
      .with(a -> {
        logger.info(a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo34(Vertx vertx, VertxTestContext testContext) throws Exception {
    UnicastProcessor<Integer> processor = UnicastProcessor.create();
    processor.onItem().transform(a -> a * 1000)
      .onFailure().recoverWithItem(1000000000);
    new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        processor.onNext(i);
      }
      processor.onComplete();
    }).start();

    Thread.sleep(10000);
    testContext.completeNow();
  }

  @Test
  public void demo35(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom()
      .range(1, 101)
      .plug(RandomDrop::new)
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo36(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items("a", "b", "c", "d")
      .onItem()
      .invoke(i -> logger.info("Receive item: {}", i))
      .onFailure()
      .invoke(t -> logger.error(t.getMessage(), t))
      .onCompletion()
      .invoke(() -> logger.info("completed"))
      .onSubscribe()
      .invoke(subscription -> logger.info("we are subscribed!"))
      .onCancellation()
      .invoke(() -> logger.info("cancelled !"))
      .onRequest()
      .invoke(n -> logger.info("Downstream request {} item", n))
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo36_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items("a", "b", "c", "d")
      .onItem()
      .invoke(i -> logger.info("Receive item: {}", i))
      .onFailure()
      .invoke(t -> logger.error(t.getMessage(), t))
      .onCompletion()
      .invoke(() -> logger.info("completed"))
      .onSubscribe()
      .invoke(subscription -> logger.info("we are subscribed!"))
      .onCancellation()
      .invoke(() -> logger.info("cancelled !"))
      .onRequest()
      .invoke(n -> logger.info("Downstream request {} item", n))
      .subscribe()
      .with(subscription -> {
        subscription.request(20);
      }, logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo36_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items("a", "b", "c", "d")
      .onItem()
      .invoke(i -> logger.info("Receive item: {}", i))
      .onFailure()
      .invoke(t -> logger.error(t.getMessage(), t))
      .onCompletion()
      .invoke(() -> logger.info("completed"))
      .onSubscribe()
      .invoke(subscription -> logger.info("we are subscribed!"))
      .onCancellation()
      .invoke(() -> logger.info("cancelled !"))
      .onRequest()
      .invoke(n -> logger.info("Downstream request {} item", n))
      .subscribe()
      .withSubscriber(new Subscriber<String>() {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
          this.subscription = subscription;
          this.subscription.request(1);
        }

        @Override
        public void onNext(String item) {
          logger.info("======== Got item: {}", item);
          subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
          logger.info("======== error: " + t.getMessage(), t);
          testContext.failNow(t);
        }

        @Override
        public void onComplete() {
          logger.info("======== completed");
          testContext.completeNow();
        }
      });
  }

  @Test
  public void demo36_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().items("a", "b", "c", "d")
      .onItem()
      .invoke(i -> logger.info("Receive item: {}", i))
      .onFailure()
      .invoke(t -> logger.error(t.getMessage(), t))
      .onCompletion()
      .invoke(() -> logger.info("completed"))
      .onSubscribe()
      .invoke(subscription -> logger.info("we are subscribed!"))
      .onCancellation()
      .invoke(() -> logger.info("cancelled !"))
      .onRequest()
      .invoke(n -> logger.info("Downstream request {} item", n))
      .subscribe()
      .withSubscriber(new Subscriber<String>() {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
          this.subscription = subscription;
          this.subscription.request(1);
        }

        @Override
        public void onNext(String item) {
          logger.info("======== Got item: {}", item);
          subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
          logger.info("======== error: " + t.getMessage(), t);
          testContext.failNow(t);
        }

        @Override
        public void onComplete() {
          logger.info("======== completed");
          testContext.completeNow();
        }
      });
  }

  // throw BackPressureFailure
  @Test
  public void demo37(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().ticks().every(Duration.ofMillis(10))
      // .onOverflow().buffer(250)
      .emitOn(Infrastructure.getDefaultExecutor())
      .onItem()
      .transform(Unchecked.function(a -> {
        Thread.sleep(1000);
        return a;
      }))
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo37_1(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().ticks().every(Duration.ofMillis(10))
      .emitOn(Infrastructure.getDefaultExecutor())
      .onItem()
      .transform(Unchecked.function(a -> {
        Thread.sleep(1000);
        return a;
      }))
      .subscribe()
      .withSubscriber(new Subscriber<Long>() {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
          this.subscription = subscription;
          this.subscription.request(1);
        }

        @Override
        public void onNext(Long item) {
          logger.info("get item: {}", item);
          this.subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
          logger.error(t.getMessage(), t);
          testContext.failNow(t);
        }

        @Override
        public void onComplete() {
          logger.info("completed");
          testContext.completeNow();
        }
      });
  }

  @Test
  public void demo37_2(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().ticks().every(Duration.ofMillis(100))
      .onOverflow().drop(x -> logger.info("Dropping item: {}", x))
      .emitOn(Infrastructure.getDefaultExecutor())
      .onItem()
      .transform(Unchecked.function(a -> {
        Thread.sleep(1000);
        return a;
      }))
      .subscribe()
      .with(logger::info, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      }, testContext::completeNow);
  }

  @Test
  public void demo37_3(Vertx vertx, VertxTestContext testContext) throws Exception {
    Multi.createFrom().ticks().every(Duration.ofMillis(10))
      .emitOn(Infrastructure.getDefaultExecutor())
      .onItem()
      .transform(Unchecked.function(a -> {
        Thread.sleep(1000);
        return a;
      }))
      .subscribe()
      .withSubscriber(new Subscriber<Long>() {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
          this.subscription = subscription;
          this.subscription.request(1);
        }

        @Override
        public void onNext(Long item) {
          logger.info("get item: {}", item);
          this.subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
          logger.error(t.getMessage(), t);
          testContext.failNow(t);
        }

        @Override
        public void onComplete() {
          logger.info("completed");
          testContext.completeNow();
        }
      });
  }

  @Test
  public void demo38() throws Exception {
    Tuple3<String, LocalDateTime, String> tuple3 = Tuple3.of("tupleTest", LocalDateTime.now(), "china");
    String name = tuple3.getItem1();
    LocalDateTime localDateTime = tuple3.getItem2();
    String address = tuple3.getItem3();
    logger.info("name: {}, location: {}, time: {}", name, address, localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  @Test
  public void demo39(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("-----");
    logger.info("hello: {}", () -> {
      logger.info("inner -----");
      String a = Uni.createFrom().item("hello").emitOn(executor).onItem().delayIt().by(Duration.ofSeconds(2)).await().indefinitely();
      logger.info("inner result -----");
      return a;
    });
    logger.info("----- end");
    Thread.sleep(5000);
    testContext.completeNow();
  }

  @Test
  public void demo40(Vertx vertx, VertxTestContext testContext) throws Exception {
    Uni.createFrom().item("hello")
      .onItem()
      .transformToUni(a -> {
        logger.info("a: {}", a);
        return Uni.createFrom().item("inner 1");
      })
      .onItem()
      .transform(a -> {
        logger.info("aa: {}", a);
        return "a: " + a;
      })
      .subscribe()
      .with(a -> {
        logger.info("aaa: {}", a);
        testContext.completeNow();
      }, t -> {
        logger.error(t.getMessage(), t);
        testContext.failNow(t);
      });
  }

  @Test
  public void demo41(Vertx vertx, VertxTestContext testContext) throws Exception {
    asyncFunc("a", "b", (a, b) -> {
      logger.info("a: {}, b: {}", a, b);
      testContext.completeNow();
    });
  }

  private void asyncFunc(String a, String b, BiConsumer<String, String> consumer) {
    consumer.accept(a, b);
  }

  private class RandomDrop<T> extends AbstractMultiOperator<T, T> {
    public RandomDrop(Multi<? extends T> upstream) {
      super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
      upstream.subscribe().withSubscriber(new DropProcessor(downstream));
    }

    private class DropProcessor extends MultiOperatorProcessor<T, T> {
      public DropProcessor(MultiSubscriber<? super T> downstream) {
        super(downstream);
      }

      @Override
      public void onItem(T item) {
        if (ThreadLocalRandom.current().nextBoolean()) {
          super.onItem(item);
        }
      }
    }
  }

  private CompletionStage<List<String>> page(int page) {
    if (page < 10) {
      List<String> list = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        list.add(Integer.toString(page * 10 + i));
      }
      return CompletableFuture.completedFuture(list);
    } else {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  private Uni<List<String>> pageUni(int page) {
    if (page < 10) {
      List<String> list = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        list.add(Integer.toString(page * 10 + i));
      }
      return Uni.createFrom().item(list);
    } else {
      return Uni.createFrom().item(Collections.emptyList());
    }
  }

  private void printWithThread(Object obj) {
    System.out.println(Thread.currentThread() + " : " + obj);
  }

  private void hello(String a) {
    System.out.println("hello world " + a);
  }


  private class PollableDataSource {
    private final AtomicInteger counter = new AtomicInteger();

    String poll() {
      block();
      if (counter.get() == 5) {
        return null;
      }
      logger.info("pool hello world");
      return Integer.toString(counter.getAndIncrement());
    }

    private void block() {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    public void close() {
      // do nothing.
    }
  }
}
