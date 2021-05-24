package org.zunpeng.vertx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.zunpeng.vertx.serivce.mysql.QPerson;

public class QueryDSLTest {

  private static final Logger logger = LogManager.getLogger(QueryDSLTest.class);

  @Test
  public void demo() {
    QPerson qPerson = QPerson.person;
    qPerson.id.eq(12L);
    qPerson.mobile.like("%a%");
  }
}
