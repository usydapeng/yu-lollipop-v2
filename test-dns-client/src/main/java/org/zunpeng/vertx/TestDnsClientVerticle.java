package org.zunpeng.vertx;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestDnsClientVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(TestDnsClientVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    WebClient webClient = WebClient.create(vertx, new WebClientOptions().setLogActivity(true));

    Multi.createFrom()
      .items(
        "api.rzrtc.com", "data-center.rzrtc.com", "snapshot.tosee.tech", "console.rzrtc.com", "www.rzrtc.com",
        "admin.rzrtc.com", "console.tosee.tech", "static.tosee.tech", "static.rzrtc.com",
        "www.duobeiyun.com", "api.duobeiyun.com", "admin.duobeiyun.com",
        "www.baidu.com", "www.qq.com", "www.sogou.com", "www.vipkid.com.cn",
        "www.52jiaoshi.com", "www.yaoguo.cn", "www.kaochong.com", "www.danglaoshi.info"
      )
      .onItem()
      .transformToUniAndConcatenate(domain -> {
        try {
          long currentTimestamp = System.currentTimeMillis();
          Uni<HttpResponse<Buffer>> uni = webClient.getAbs("https://" + domain + "/").send();
          logger.info("----- domain: {}, time: {}", domain, System.currentTimeMillis() - currentTimestamp);
          return uni;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      })
      .onFailure().retry().atMost(2)
      .subscribe()
      .with(response -> logger.info("------ result: {}", response.statusCode()),
        throwable -> logger.error(throwable.getMessage(), throwable),
        () -> {
          startPromise.complete();
          vertx.close()
            .subscribe()
            .with(r -> logger.info("--------------- completed ---------------"), t -> logger.error(t.getMessage(), t));
        });
  }
}
