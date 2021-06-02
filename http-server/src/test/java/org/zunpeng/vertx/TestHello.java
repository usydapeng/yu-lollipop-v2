package org.zunpeng.vertx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.DoubleToLongFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;

public class TestHello {

  private static final Logger logger = LogManager.getLogger(TestHello.class);

  @Test
  public void demo1() {
    logger.info("a a a a: {}", 1);
  }

  @Test
  public void demo2() {
    logger.info("size: {}", testFunc("zhangsan", String::length));
    logger.info("size: {}", testFunc("zhangsan", (a) -> a.length()));

    TriConsumer<String, Integer, Object> consumer = (a, b, c) -> {
      logger.info(a + " " + b + " " + c);
      return;
    };
    consumer.accept("hello", 1, "obj");

    RetConsumer<String, Integer, Object, String> con = (a, b, c) -> {
      String m = a + " " + b + " " + c;
      logger.info("m: {}", m);
      return m;
    };
    String ret = con.apply("world", 2, "oo");
    logger.info("ret: {}", ret);

    DoubleToLongFunction doubleToLongFunction = (a) -> new Double(a).longValue();
    LongSupplier longSupplier = () -> 1000;
    logger.info(longSupplier.getAsLong());

    testFunc("lisi", null);
  }

  @Test
  public void demo3() {
    BiPredicate<String, String> predicate = (a, b) -> {
      return a.length() > b.length();
    };
    logger.info("predicate: {}", predicate.test("a", "ab"));
    logger.info("predicate: {}", predicate.test("aab", "ab"));
  }

  private Integer testFunc(String msg, Function<String, Integer> func) {
    Objects.requireNonNull(func);
    return func.apply(msg);
  }
}
