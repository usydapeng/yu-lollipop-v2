package org.zunpeng.vertx.service.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.draft7.dsl.Keywords;
import io.vertx.json.schema.draft7.dsl.Schemas;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.core.ServerConstant;
import org.zunpeng.vertx.service.mysql.MysqlService;
import org.zunpeng.vertx.service.redis.RedisService;
import org.zunpeng.vertx.service.redis.User;

import java.time.LocalDateTime;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(HttpServerVerticle.class);

  @Override
  public void start(Promise<Void> promise) {
    logger.info("config of httpServerVerticle: {}", config().encode());

    RedisService redisService = org.zunpeng.vertx.service.redis.RedisService.createProxy(vertx, ServerConstant.EVENT_BUS_REDIS_ADDR);
    MysqlService mysqlService = org.zunpeng.vertx.service.mysql.MysqlService.createProxy(vertx, ServerConstant.EVENT_BUS_MYSQL_ADDR);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route("/favicon.ico").handler(routingContext -> routingContext.end("he"));

    router.mountSubRouter("/validate", buildValidateRouter());

    router.get("/test").handler(routingContext ->
      vertx.setTimer(5000, timerId -> {
        routingContext.end("20");
      }));

    router.get("/redis").handler(routingContext -> {
      redisService.get("hello", ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
          routingContext.fail(ar.cause());
        } else {
          routingContext.end(ar.result() == null ? "is null" : ar.result());
        }
      });
    });

    router.get("/redis/list").handler(routingContext -> {
      redisService.list("hello-list", ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
          routingContext.fail(ar.cause());
        } else {
          routingContext.end(ar.result().toString());
        }
      });
    });

    router.get("/user").handler(routingContext -> {
        // User otherUser = new User(100L, "三", "张");
        User otherUser = new User().setId(100L).setFirstName("三").setLastName("张");
        redisService.testUser(otherUser, ar -> {
          if (ar.failed()) {
            logger.error(ar.cause().getMessage(), ar.cause());
            routingContext.fail(ar.cause());
          } else {
            routingContext.end(ar.result().toJson().encode());
          }
        });
      });

    router.get("/device").handler(routingContext -> {
      mysqlService.listDevice(ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
          routingContext.fail(ar.cause());
        } else {
          JsonArray jsonArray = new JsonArray();
          ar.result().forEach(device -> jsonArray.add(device.toJson()));
          routingContext.end(jsonArray.encode());
        }
      });
    });

    router.get("/device/add").handler(routingContext -> {
        mysqlService.saveDevice(LocalDateTime.now().toString(), ar -> {
          if (ar.failed()) {
            logger.error(ar.cause().getMessage(), ar.cause());
            routingContext.fail(ar.cause());
          } else {
            routingContext.end(ar.result().toJson().encode());
          }
        });
      });

    router.get("/device/update").handler(routingContext -> {
      mysqlService.updateDevice(1L, "lzhangsan", ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
          routingContext.fail(ar.cause());
        } else {
          routingContext.end("success");
        }
      });
    });

    router.get("/device/update-tx").handler(routingContext -> {
      mysqlService.txUpdateDevice(1L, "sdsdsd", ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
          routingContext.fail(ar.cause());
        } else {
          routingContext.end("success");
        }
      });
    });

    router.get("/device/:deviceId").handler(routingContext -> {
      Long deviceId = Long.valueOf(routingContext.pathParam("deviceId"));
      mysqlService.getDeviceById(deviceId, ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
          routingContext.fail(ar.cause());
        } else {
          routingContext.end(ar.result().toJson().encode());
        }
      });
    });

    router.route("/*").handler(routingContext -> {
      logger.info("hello world");
      routingContext.end("hello world");
    });

    router.errorHandler(400, routingContext -> {
      Throwable throwable = routingContext.failure();
      logger.error(throwable.getMessage(), throwable);
      logger.info("class: {}", throwable.getClass());
      if (throwable instanceof ParameterProcessorException) {
        ParameterProcessorException parameterProcessorException = (ParameterProcessorException) throwable;
        logger.info("error json: {}, parameterName: {}, errorType: {}, location: {}, errorCause: {}, msg: {}",
          parameterProcessorException.toJson().encode(), parameterProcessorException.getParameterName(),
          parameterProcessorException.getErrorType(), parameterProcessorException.getLocation(),
          parameterProcessorException.getCause().getMessage(), parameterProcessorException.getMessage());
        logger.info("{} in {}: {}", parameterProcessorException.getParameterName(),
          parameterProcessorException.getLocation(), parameterProcessorException.getCause().getMessage());

        JsonObject errorJson = new JsonObject()
          .put("parameterName", parameterProcessorException.getParameterName())
          .put("location", parameterProcessorException.getLocation().toString())
          .put("error", parameterProcessorException.getCause().getMessage());
        JsonObject jsonObject = new JsonObject().put("code", 400).put("msg", parameterProcessorException.getMessage()).put("data", errorJson);

        routingContext.response().setStatusCode(400).end(jsonObject.encode());
        return;
      }
      routingContext.response().setStatusCode(400).end("400, 400, 400");
    });

    vertx.createHttpServer().requestHandler(router).listen(8888)
      .onSuccess(http -> {
        logger.info("======== port: {}", http.actualPort());
        promise.complete();
      })
      .onFailure(throwable -> {
        logger.error(throwable.getMessage(), throwable);
        promise.fail(throwable);
      });
  }

  private Router buildValidateRouter() {
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createDraft201909SchemaParser(schemaRouter);

    Router router = Router.router(vertx);

    router.get("/a")
      .handler(
        ValidationHandler.builder(schemaParser)
          .queryParameter(Parameters.param("a", Schemas.intSchema().with(Keywords.maximum(100)).with(Keywords.minimum(90))))
          .queryParameter(Parameters.param("b", Schemas.intSchema().with(Keywords.maximum(10)).with(Keywords.minimum(5))))
          .build()
      )
      .handler(routingContext -> {
        try {
          logger.info("===============================");
          RequestParameters parameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
          logger.info("request parameter: {}", parameters.toJson().encode());
          int a = parameters.queryParameter("a").getInteger();
          routingContext.end(Integer.toString(a * 10));
        } catch (Throwable t) {
          logger.error(t.getMessage(), t);
          routingContext.fail(t);
        }
      });

    // TODO important 此处无效，需要放到根router上
    // router.errorHandler(400, routingContext -> {
    //   logger.error("----------------------------");
    //   Throwable throwable = routingContext.failure();
    //   logger.error(throwable.getMessage(), throwable);
    //   routingContext.response().setStatusCode(400).end();
    // });

    router.route().handler(routingContext -> routingContext.end("hello validate"));
    return router;
  }
}
