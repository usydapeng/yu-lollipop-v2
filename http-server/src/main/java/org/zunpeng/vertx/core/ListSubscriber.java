package org.zunpeng.vertx.core;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

public class ListSubscriber<T> implements org.reactivestreams.Subscriber<T> {

  private static final Logger logger = LogManager.getLogger(ListSubscriber.class);

  private final List<T> List = new ArrayList<>();
  private final Handler<AsyncResult<List<T>>> handler;
  private Subscription subscription;

  public ListSubscriber(Handler<AsyncResult<List<T>>> handler) {
    this.handler = handler;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;
    this.subscription.request(1);
  }

  @Override
  public void onNext(T t) {
    List.add(t);
    this.subscription.request(1);
  }

  @Override
  public void onError(Throwable t) {
    logger.error(t.getMessage(), t);
    handler.handle(Future.failedFuture(t));
  }

  @Override
  public void onComplete() {
    handler.handle(Future.succeededFuture(List));
  }
}
