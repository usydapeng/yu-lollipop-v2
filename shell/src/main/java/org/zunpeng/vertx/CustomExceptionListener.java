package org.zunpeng.vertx;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ExceptionListener;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class CustomExceptionListener implements ExceptionListener {

  private static final Logger logger = LogManager.getLogger(CustomExceptionListener.class);

  @Override
  public void onEventException(Exception exception, List<Object> args, SocketIOClient client) {
    logger.info(exception.getMessage(), exception);
    logger.info("args: {}", args);
  }

  @Override
  public void onDisconnectException(Exception exception, SocketIOClient client) {
    logger.info(exception.getMessage(), exception);
  }

  @Override
  public void onConnectException(Exception exception, SocketIOClient client) {
    logger.info(exception.getMessage(), exception);
  }

  @Override
  public void onPingException(Exception exception, SocketIOClient client) {
    logger.info(exception.getMessage(), exception);
  }

  @Override
  public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) throws Exception {
    logger.info(throwable.getMessage(), throwable);
    return false;
  }
}
