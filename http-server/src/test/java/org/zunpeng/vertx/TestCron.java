package org.zunpeng.vertx;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Locale;

@ExtendWith(VertxExtension.class)
public class TestCron {

  private static final Logger logger = LogManager.getLogger(TestCron.class);

  private ExecutionTime executionTime;

  @BeforeEach
  public void beforeClass() {
    String cronExpression = "*/1 * * * * ?";
    CronDescriptor cronDescriptor = CronDescriptor.instance(Locale.SIMPLIFIED_CHINESE);
    CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
    CronParser cronParser = new CronParser(cronDefinition);
    Cron cron = cronParser.parse(cronExpression);
    executionTime = ExecutionTime.forCron(cron);
    CronDescriptor.instance(Locale.SIMPLIFIED_CHINESE);

    logger.info("expression: {}, mean: {}", cronExpression, cronDescriptor.describe(cron));
  }

  public void schedule(Vertx vertx, Handler<AsyncResult<Long>> handler) {
    executionTime.nextExecution(ZonedDateTime.now())
      .ifPresent(time -> {
        Duration timeToNextExecution = Duration.between(ZonedDateTime.now(), time);
        long delayMillis = Math.abs(Math.max(1, timeToNextExecution.toMillis()));
        vertx.setTimer(delayMillis, timerId -> {
          logger.info("hello world: {}", timerId);
          handler.handle(Future.succeededFuture(timerId));
          schedule(vertx, handler);
        });
      });
  }

  @Test
  public void demo(Vertx vertx, VertxTestContext testContext) {
    schedule(vertx, ar -> {
      if (ar.failed()) {
        logger.error(ar.cause().getMessage(), ar.cause());
        testContext.failNow(ar.cause());
      } else {
        logger.info(ar.result());
      }
    });
  }
}
