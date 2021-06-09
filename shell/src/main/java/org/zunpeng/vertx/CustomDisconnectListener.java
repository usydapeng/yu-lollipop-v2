package org.zunpeng.vertx;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomDisconnectListener implements DisconnectListener {

  private static final Logger logger = LogManager.getLogger(CustomDisconnectListener.class);

  @Override
  public void onDisconnect(SocketIOClient client) {
    logger.info("--------------- disconnect: {}, {}, {}",
      client.getSessionId(), client.getHandshakeData().getUrl(), client.getHandshakeData().getUrlParams());
  }
}
