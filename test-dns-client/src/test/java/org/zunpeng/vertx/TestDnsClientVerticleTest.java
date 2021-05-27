package org.zunpeng.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mutiny.core.MultiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.zunpeng.vertx.core.DesEnAndDe;
import org.zunpeng.vertx.core.EncryptUtil;

import java.net.InetAddress;
import java.util.Arrays;

@ExtendWith(VertxExtension.class)
class TestDnsClientVerticleTest {

  private static final Logger logger = LogManager.getLogger(TestDnsClientVerticleTest.class);

  @Test
  public void demo(Vertx vertx, VertxTestContext testContext) {
    vertx.createDnsClient(new DnsClientOptions().setLogActivity(true))
      .lookup("www.baidu.com")
      .onSuccess(result -> {
        logger.info("---- result: {}", result);
        testContext.completeNow();
      })
      .onFailure(throwable -> {
        logger.error(throwable.getMessage());
        testContext.failNow(throwable);
      });
  }

  @Test
  public void demo1() throws Exception {
    InetAddress[] inetAddresses = InetAddress.getAllByName("api.rzrtc.com");
    for (InetAddress inetAddress : inetAddresses) {
      logger.info(inetAddress.getHostAddress());
    }
  }

  @RepeatedTest(100)
  public void demo2(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx, new WebClientOptions().setLogActivity(true));
    long currentTimestamp = System.currentTimeMillis();
    webClient.getAbs("https://api1.tosee.tech/api/v1/app/version?platform=0").send()
      .onSuccess(rs -> {
        logger.info("------ time: {}, request-id: {}", (System.currentTimeMillis() - currentTimestamp), rs.headers().get("X-Request-Id"));
        testContext.completeNow();
      })
      .onFailure(throwable -> {
        logger.error(throwable.getMessage(), throwable);
        testContext.failNow(throwable);
      });
  }

  @Test
  public void demo3(Vertx vertx, VertxTestContext testContext) throws Exception {
    // System.setProperty("java.net.preferIPv4Stack", "true");
    // System.setProperty("java.net.preferIPv6Addresses", "true");
    // Vertx vertx = Vertx.vertx(new VertxOptions().setAddressResolverOptions(new AddressResolverOptions().addServer("114.114.114.114")));
    Vertx verxtx = Vertx.vertx();
    WebClient webClient = WebClient.create(vertx, new WebClientOptions().setConnectTimeout(100).setLogActivity(true));

    webClient.getAbs("https://api1.tosee.tech/api/v1/user/info").send()
      .onSuccess(rs -> {
        logger.info("------ {}", rs);
      })
      .onFailure(throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });
    Thread.sleep(10000);
  }

  @Test
  public void demo4(Vertx _vertx, VertxTestContext testContext) throws Exception {
    io.vertx.mutiny.core.Vertx vertx = new io.vertx.mutiny.core.Vertx(_vertx);

    MultiMap parameterMap = MultiMap.caseInsensitiveMultiMap();
    // parameterMap.add("dn", getVa("api.rzrtc.com"));
    parameterMap.add("dn", "www.baidu.com");
    // parameterMap.add("id", "7936");
    // parameterMap.add("alg", "aes");
    parameterMap.add("token", "879311147");
    parameterMap.add("query", "1");
    parameterMap.add("timeout", "1000");
    parameterMap.add("ttl", "1");
    parameterMap.add("type", "A");
    parameterMap.add("clientip", "1");

    StringBuffer sb = new StringBuffer("https://119.29.29.99/d?");
    // StringBuffer sb = new StringBuffer("http://119.29.29.98/d?");
    parameterMap.forEach(entry -> {
      if (!sb.toString().endsWith("?")) {
        sb.append("&");
      }
      sb.append(entry.getKey()).append("=").append(entry.getValue());
    });

    logger.info(sb.toString());

    io.vertx.mutiny.ext.web.client.WebClient webClient = io.vertx.mutiny.ext.web.client.WebClient.create(vertx, new WebClientOptions().setLogActivity(true));
    webClient.getAbs(sb.toString())
      .putHeader("Host", "cloud.tencent.com")
      .sendForm(parameterMap)
      .subscribe()
      .with(rs -> {
        logger.info(rs.headers());
        logger.info(rs.body());
        testContext.completeNow();
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
        testContext.failNow(throwable);
      });
  }

  private String getVa(String value) {
    // return Objects.requireNonNull(DesEnAndDe.DES_CBC_Encrypt("oLAbiNY1", value)).toLowerCase(Locale.ROOT);
    return EncryptUtil.encryptHex(value, "vWKox6YUCaQKiUeM");
  }

  @Test
  public void demo23() {
    logger.info(DesEnAndDe.DES_CBC_Decrypt("oLAbiNY1", "3983a6ff9f428a98b1aa6e1fc1c5cb72f0da09e5763c7899"));
  }

  @Test
  public void demo233() throws Exception {
    InetAddress[] inetAddresses = InetAddress.getAllByName("210.73.212.120");
    System.out.println(Arrays.asList(inetAddresses));
  }

  @Test
  public void demo5(Vertx vertx, VertxTestContext testContext) throws Exception {
    logger.info("timer start");
    vertx.setTimer(1000, timerId -> {
      logger.info("timeId: {}", timerId);
      testContext.completeNow();
    });
  }
}
