package org.zunpeng.vertx;

import duobei.TestVm;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.datagram.DatagramSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class TestVMTest {

  private static final Logger logger = LogManager.getLogger(TestVMTest.class);

  @Test
  public void demo() throws Exception {
    Vertx vertx = Vertx.vertx();
    TestVm.vmrequest vmrequest = TestVm.vmrequest.newBuilder()
      .setRoomid("roomid")
      .setVersion(1)
      .setFilling("zhangsan")
      .build();

    Buffer buffer = Buffer.buffer(vmrequest.toByteArray());
    System.out.println(":::: " + buffer);
    System.out.println(":::: " + vmrequest.toByteArray().length);
    System.out.println("-------------------------------- --------------------------------");
    DatagramSocket datagramSocket = vertx.createDatagramSocket(new DatagramSocketOptions().setLogActivity(true));
    datagramSocket.send(buffer, 8990, "188.131.153.111")
      .subscribe()
      .with(m -> {
        logger.info("======= udp connect success");
        datagramSocket.handler(packet -> {
          Buffer replyBuffer = packet.data();
          logger.info("======= udp reply: {}", replyBuffer.getBytes().length);
          try {
            TestVm.rzresponse response = TestVm.rzresponse.parseFrom(replyBuffer.getBytes());
            logger.info("======= udp response: {}", response);
          } catch (Throwable t) {
            logger.error(t.getMessage(), t);
          }
        });
      }, throwable -> {
        logger.error(throwable.getMessage(), throwable);
      });

    Thread.sleep(20000);
  }
}
