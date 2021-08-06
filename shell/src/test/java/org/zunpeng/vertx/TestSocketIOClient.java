package org.zunpeng.vertx;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

@ExtendWith(VertxExtension.class)
public class TestSocketIOClient {

  private static final Logger logger = LogManager.getLogger(TestSocketIOClient.class);

  private static String preTokenPattern = "roomId=%s&uid=%s%s";
  private static String tokenPattern = "roomId=%s&uid=%s&token=%s";
  private static String queryPattern = "app=%s&roomId=%s&token=%s";
  private String secretKey = "jhsjaaaai8y2193h--dvv78bydf78n2k";
  // private String address = "https://tinderdev-sio-bj.duobeiyun.com:20081";
  private String address = "http://127.0.0.1:3001";
  private OkHttpClient okHttpClient;

  private String buildQuery(String roomId, String uid, String secretKey) throws Exception {
    String preToken = String.format(preTokenPattern, roomId, uid, secretKey);
    String token = md5(preToken);
    String tokenParam = String.format(tokenPattern, roomId, uid, token);
    return String.format(queryPattern, "zwt", roomId, URLEncoder.encode(tokenParam, "UTF-8"));
  }

  public String md5(String src) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    return new String(Hex.encodeDigest(md.digest(src.getBytes())));
  }

  private static final class Hex {

    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static char[] encodeDigest(byte[] bytes) {
      final int nBytes = bytes.length;
      char[] result = new char[2 * nBytes];

      int j = 0;
      for (int i = 0; i < nBytes; i++) {

        int n = bytes[i];
        if (n < 0) {
          n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;

        result[j++] = HEX[d1];
        result[j++] = HEX[d2];
      }
      return result;
    }
  }

  @BeforeEach
  public void before() throws Exception {
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(100);
    dispatcher.setMaxRequestsPerHost(100);
    okHttpClient = new OkHttpClient.Builder().dispatcher(dispatcher).build();
    IO.setDefaultOkHttpCallFactory(okHttpClient);
    IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
  }

  private Socket buildSocket(String uid) throws Exception {
    IO.Options options = new IO.Options();
    options.timeout = 3000;
    options.reconnection = true;
    options.query = buildQuery("roomIds", uid, secretKey);
    options.transports = new String[]{WebSocket.NAME};
    options.forceNew = true;
    options.callFactory = okHttpClient;
    options.webSocketFactory = okHttpClient;

    Socket socket = IO.socket(address, options);
    socket.on(io.socket.engineio.client.Socket.EVENT_PONG, args -> {
      logger.info("uid: {}, {}, pong: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(io.socket.engineio.client.Socket.EVENT_PING, args -> {
      logger.info("uid: {}, {}, ping: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(io.socket.engineio.client.Socket.EVENT_CLOSE, args -> {
      logger.info("uid: {}, {}, event close: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(io.socket.engineio.client.Socket.EVENT_DATA, args -> {
      logger.info("uid: {}, {}, event data: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(io.socket.engineio.client.Socket.EVENT_UPGRADE, args -> {
      logger.info("uid: {}, {}, upgrade: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(Socket.EVENT_CONNECT, args -> {
      logger.info("uid: {}, {}, connect: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
      logger.info("uid: {}, {}, connect error: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on(Socket.EVENT_DISCONNECT, args -> {
      logger.info("uid: {}, {}, disconnect: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on("DeviceStatusChange", args -> {
      logger.info("uid: {}, {}, device status change received: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on("DeviceStatusQuery", args -> {
      logger.info("uid: {}, {}, device status query received: {}", uid, socket.id(), Arrays.toString(args));
    });
    socket.on("CommonMsg", args -> {
      logger.info("uid: {}, {}, device received command msg: {}", uid, socket.id(), Arrays.toString(args));
    });
    return socket;
  }

  @Test
  public void demo(Vertx vertx, VertxTestContext testContext) throws Exception {
    Socket webSocket = buildSocket("web");
    buildSocket("device_1").connect();

    vertx.setTimer(3000, timerId -> {
      try {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deviceAlias", "device_1");
        jsonObject.put("deviceInfos", new JSONArray().put(new JSONObject().put("deviceAlias", "device_1").put("voiceStatus", 1).put("videoStatus", 0)));
        jsonObject.put("deviceConfig", new JSONObject().put("isstudent", true).toString());

        // JSONObject js = new JSONObject("{\"deviceAlias\":\"wlj_yn_ml_s_01\",\"deviceInfos\":[{\"deviceAlias\":\"wlj_yn_ml_t_01\",\"voiceStatus\":1,\"videoStatus\":1}],\"deviceConfig\":{\"isstudent\":true,\"isrecvppt\":false,\"issendppt\":false,\"pptDeviceAlias\":\"wlj_yn_ml_t_01\",\"issendaudio\":true,\"issendvideo\":true,\"machineConfig\":null,\"clusterInfoMsg\":[{\"ip\":\"140.143.29.74\",\"port\":\"8021\"},{\"ip\":\"129.211.15.158\",\"port\":\"8021\"},{\"ip\":\"203.195.160.180\",\"port\":\"8021\"},{\"ip\":\"148.70.187.7\",\"port\":\"8021\"},{\"ip\":\"94.191.52.49\",\"port\":\"8021\"}],\"deviceChinesizationMsg\":[{\"alias\":\"wlj_yn_ml_s_01\",\"chinesizationName\":\"21200,33098,26657,21306,39640,19968,52,29677\",\"nameCh\":\"勐腊校区高一4班\"},{\"alias\":\"wlj_yn_ml_t_01\",\"chinesizationName\":\"21200,33098,26657,21306,20116,27004,30452,25773,38388\",\"nameCh\":\"勐腊校区五楼直播间\"}]}}");
        webSocket.emit("command", "DeviceStatusChange", jsonObject, (Ack) args -> {
          logger.info("device status change send: {}", Arrays.toString(args));
        });
      } catch(Throwable t) {
        logger.error(t.getMessage(), t);
      }
    });
    vertx.setTimer(5000, timerId -> {
      try {
        webSocket.emit("command", "SendToDevice", new JSONObject().put("deviceAlias", "device_1").put("msg", "hello world"), (Ack) args -> {
          logger.info("server send msg to Device: {}", Arrays.toString(args));
        });
      } catch (Exception t) {
        logger.error(t.getMessage(), t);
      }
    });
    // vertx.setPeriodic(2000, timerId -> {
    //   socket.emit("command", "OnlineDevices", null, (Ack) args -> {
    //     logger.info("socket id: {},,, device status query ack: {}", socket.id(), Arrays.toString(args));
    //   });
    // });
    webSocket.connect();
    logger.info("-------------------------");
    vertx.setTimer(29000, timerId -> {
      logger.info("timer id: {}", timerId);
      testContext.completeNow();
    });
  }

  @Test
  public void demo2() {
    Arrays.asList(1, 2, 3).forEach(i -> {
      System.out.println(i);
      if (i == 1) {
        return;
      }
      System.out.println(i * 10);
    });
  }

  public void demo3() {
    List<Integer> a = Arrays.asList(1, 2, 3);
    List<Integer> aList = a;
    aList.forEach(a1 -> {

    });
    aList.forEach(b -> {

    });
    aList.forEach(b -> {

    });
  }
}
