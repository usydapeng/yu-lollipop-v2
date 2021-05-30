package org.zunpeng.vertx.service.mysql.domain;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.format.SnakeCase;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.annotations.ParametersMapped;
import io.vertx.sqlclient.templates.annotations.RowMapped;

@DataObject(generateConverter = true, publicConverter = true)
@ParametersMapped
@RowMapped(formatter = SnakeCase.class)
public class Device {

  private Long id;

  private String sn;

  public Device(){}

  public Device(Long id, String sn) {
    this.id = id;
    this.sn = sn;
  }

  public Device(JsonObject jsonObject) {
    DeviceConverter.fromJson(jsonObject, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    DeviceConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public Long getId() {
    return id;
  }

  @Fluent
  public Device setId(Long id) {
    this.id = id;
    return this;
  }

  public String getSn() {
    return sn;
  }

  @Fluent
  public Device setSn(String sn) {
    this.sn = sn;
    return this;
  }
}
