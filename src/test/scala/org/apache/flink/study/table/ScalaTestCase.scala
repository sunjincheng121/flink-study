package org.apache.flink.study.table

import org.junit.Test
import org.apache.flink.api.scala._

class ScalaTestCase {

  @Test
  def test(): Unit = {
    val c = new TestClass
    c.test(classOf[String])
  }
}
