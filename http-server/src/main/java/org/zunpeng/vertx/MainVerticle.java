package org.zunpeng.vertx;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.config.ConfigRetriever;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zunpeng.vertx.service.http.HttpServerVerticle;
import org.zunpeng.vertx.service.redis.RedisVerticle;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    String env = config().getString("lollipop.env", "local");
    ConfigStoreOptions sysConfigStoreOptions = new ConfigStoreOptions().setType("sys").setOptional(false);
    ConfigStoreOptions fileConfigStoreOptions = new ConfigStoreOptions().setType("file").setFormat("hocon")
      .setOptional(false).setConfig(new JsonObject().put("path", "conf/lollipop-" + env + ".conf"));
    ConfigRetrieverOptions configRetrieverOptions =
      new ConfigRetrieverOptions()
        .addStore(sysConfigStoreOptions)
        .addStore(fileConfigStoreOptions);

    ConfigRetriever.create(vertx, configRetrieverOptions).getConfig()
      .onItem()
      .transformToUni(configJsonObject -> {
        logger.info("config json: {}", configJsonObject);
        return vertx.deployVerticle(RedisVerticle.class.getName(),
            new DeploymentOptions().setInstances(1).setConfig(configJsonObject.getJsonObject("redis")))
          .onItem()
          .transformToUni(deploymentId -> {
            logger.info("redis deployment id: {}", deploymentId);
            return vertx.deployVerticle(HttpServerVerticle.class.getName(),
              new DeploymentOptions().setInstances(1).setConfig(configJsonObject.getJsonObject("http-server")));
          })
          .onItem()
          .transformToUni(deploymentId -> {
            logger.info("http server deployment id: {}", deploymentId);
            return Uni.createFrom().item("deploy success");
          });
      })
      .subscribe()
      .with(msg -> {
        logger.info("all deployment msg: {}", msg);
        startPromise.complete();
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        startPromise.fail(throwable);
      });

  }
}