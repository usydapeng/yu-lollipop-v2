package org.zunpeng.vertx;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomConnectListener implements ConnectListener {

  private static final Logger logger = LogManager.getLogger(CustomConnectListener.class);

  @Override
  public void onConnect(SocketIOClient client) {
    logger.info("--------------- connect: {}", client.getSessionId());
  }
}
