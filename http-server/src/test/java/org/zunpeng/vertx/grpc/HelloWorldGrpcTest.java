package org.zunpeng.vertx.grpc;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

public class HelloWorldGrpcTest {

  private static final Logger logger = LogManager.getLogger(HelloWorldGrpcTest.class);

  @Test
  public void server_demo() throws Exception {
    BindableService service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(
          HelloReply.newBuilder().setMessage("Hello: " + request.getName()).build()
        );
      }
    };

    Vertx vertx = Vertx.vertx();

    VertxServerBuilder
      .forAddress(vertx, "127.0.0.1", 8085)
      .addService(service)
      .build()
      .start(ar -> {
        if (ar.failed()) {
          logger.error("Could not start server " + ar.cause().getMessage());
          logger.error(ar.cause().getMessage(), ar.cause());
        } else {
          logger.info("gRPC service started");
        }
      });

    Thread.sleep(200000);
  }

  @Test
  public void client_demo() throws Exception {
    Vertx vertx = Vertx.vertx();

    ManagedChannel channel = VertxChannelBuilder
      .forAddress(vertx, "127.0.0.1", 8085)
      .usePlaintext()
      .build();

    VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);

    HelloRequest request = HelloRequest.newBuilder().setName("ZhangSan").build();
    stub.sayHello(request)
      .onComplete(ar -> {
        if (ar.failed()) {
          logger.error(ar.cause().getMessage(), ar.cause());
        } else {
          logger.info("succeeded " + ar.result().getMessage());
        }
      });
    Thread.sleep(2000);
  }

  @Test
  public void client_demo2() throws Exception {
    Vertx vertx = Vertx.vertx();

    ManagedChannel channel = VertxChannelBuilder
      .forAddress(vertx, "127.0.0.1", 8085)
      .usePlaintext()
      .build();

    Class clazz = Class.forName("io.grpc.examples.helloworld.HelloRequest");
    Method newBuildMethod = clazz.getMethod("newBuilder");
    Object builder = newBuildMethod.invoke(null);
    Class builderClazz = builder.getClass();
    Method setMethod = builderClazz.getMethod("setName", String.class);
    setMethod.invoke(builder, "Lisi");
    Method buildMethod = builderClazz.getMethod("build");
    Object requestObj = buildMethod.invoke(builder);
    logger.info("request obj: {}", requestObj);


    // VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
    Class grpcClazz = Class.forName("io.grpc.examples.helloworld.VertxGreeterGrpc");
    Method newStubMethod = grpcClazz.getMethod("newVertxStub", Channel.class);
    Object stub = newStubMethod.invoke(null, channel);
    Method invokeMethod = stub.getClass().getMethod("sayHello", Class.forName("io.grpc.examples.helloworld.HelloRequest"));
    invokeMethod.invoke(stub, requestObj);
    Thread.sleep(10000);
  }

  @Test
  public void getAllClass() throws Exception {
    String packageName = "io.grpc.examples.helloworld";
    packageName = packageName.replace(".", "/");
    URL url = Thread.currentThread().getContextClassLoader().getResource(packageName);
    logger.info("url: {}", url);
    URI uri = url.toURI();
    File root = new File(uri);
    File[] fileList = root.listFiles();
    for (File file: fileList) {
      logger.info("file: {}", file.getName());
    }
  }
}
