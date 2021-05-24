package org.zunpeng.vertx.serivce.mysql;

import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceCapable;

@PersistenceCapable
public class Person {

  @Key
  private Long id;

  private String mobile;

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }
}
