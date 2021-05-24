package org.zunpeng.vertx.service.redis;

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

  private String firstName;

  private String lastName;

  public User() {}

  public User(Long id, String firstName, String lastName) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public User(JsonObject json) {
    UserConverter.fromJson(json, this);
  }

  public User(User other) {
    this.id = other.id;
    this.firstName = other.firstName;
    this.lastName = other.lastName;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    UserConverter.toJson(this, json);
    return json;
  }

  public Long getId() {
    return id;
  }

  @Fluent
  public User setId(Long id) {
    this.id = id;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  @Fluent
  public User setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  @Fluent
  public User setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }
}
