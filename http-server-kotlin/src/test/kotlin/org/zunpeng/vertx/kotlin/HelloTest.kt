package org.zunpeng.vertx.kotlin

import org.junit.jupiter.api.Test

class HelloTest {

  fun inc(num : Int) {
    val num = 2
    if (num > 0) {
      val num = 3
    }
    println ("num: $num")
  }

  @Test
  fun demo() {
    inc(100)
  }
}
