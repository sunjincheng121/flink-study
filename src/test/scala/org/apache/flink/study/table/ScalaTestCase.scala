package org.apache.flink.study.table

import org.junit.Test
import org.apache.flink.api.scala._

case class P(a: Int, b: String)

class ScalaTestCase {

  @Test
  def test(): Unit = {
    val c = new TestClass
    c.test(classOf[String])
  }

  @Test
  def test2(): Unit = {
    val c = P(0, "")
    val b = 1
    val t = ScalaTypeExtractor.createTypeInfo(P)
    println(t)
  }
}
