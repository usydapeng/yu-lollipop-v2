package org.zunpeng.vertx;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomAuthorizationListener implements AuthorizationListener {

  private static final Logger logger = LogManager.getLogger(CustomAuthorizationListener.class);

  @Override
  public boolean isAuthorized(HandshakeData data) {
    logger.info("auth data: {}, params: {}", data.getUrl(), data.getUrlParams());
    return true;
  }
}
