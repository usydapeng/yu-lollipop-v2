package org.zunpeng.vertx.kotlin.service.proxy;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.format.SnakeCase;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.annotations.ParametersMapped;
import io.vertx.sqlclient.templates.annotations.RowMapped;

@DataObject(generateConverter = true, publicConverter = true)
@ParametersMapped
@RowMapped(formatter = SnakeCase.class)
public class User {

  private Long id;

  private String sn;

  public User() {
  }

  public User(Long id, String sn) {
    this.id = id;
    this.sn = sn;
  }

  public User(JsonObject jsonObject) {
    UserConverter.fromJson(jsonObject, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    UserConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public Long getId() {
    return id;
  }

  @Fluent
  public User setId(Long id) {
    this.id = id;
    return this;
  }

  public String getSn() {
    return sn;
  }

  @Fluent
  public User setSn(String sn) {
    this.sn = sn;
    return this;
  }
}
