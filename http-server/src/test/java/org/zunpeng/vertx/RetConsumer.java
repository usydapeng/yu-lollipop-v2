package org.zunpeng.vertx;

@FunctionalInterface
public interface RetConsumer<T, U, V, R> {
  R apply(T t, U u, V v);
}
