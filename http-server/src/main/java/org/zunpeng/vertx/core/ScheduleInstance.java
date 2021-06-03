package org.zunpeng.vertx.core;

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
import io.vertx.mutiny.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Locale;

public class ScheduleInstance {

  private static final Logger logger = LogManager.getLogger(ScheduleInstance.class);

  private final CronDescriptor cronDescriptor = CronDescriptor.instance(Locale.SIMPLIFIED_CHINESE);
  private final CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
  private final CronParser cronParser = new CronParser(cronDefinition);
  private final Vertx vertx;

  public ScheduleInstance(Vertx vertx) {
    this.vertx = vertx;
    CronDescriptor.instance(Locale.SIMPLIFIED_CHINESE);
  }

  public void cron(String cronExpression, Handler<AsyncResult<Long>> handler) {
    Cron cron = cronParser.parse(cronExpression);
    logger.info("expression: {}, mean: {}", cronExpression, cronDescriptor.describe(cron));
    ExecutionTime executionTime = ExecutionTime.forCron(cron);
    timer(executionTime, handler);
  }

  private void timer(ExecutionTime executionTime, Handler<AsyncResult<Long>> handler) {
    executionTime.nextExecution(ZonedDateTime.now())
      .ifPresent(time -> {
        Duration timeToNextExecution = Duration.between(ZonedDateTime.now(), time);
        long delayMillis = Math.abs(Math.max(1, timeToNextExecution.toMillis()));
        vertx.setTimer(delayMillis, timerId -> {
          handler.handle(Future.succeededFuture(timerId));
          timer(executionTime, handler);
          vertx.cancelTimer(timerId);
        });
      });
  }
}
