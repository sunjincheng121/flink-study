package org.apache.flink.study.table

import org.junit.Test
import org.apache.flink.api.scala._
import com.softwaremill.debug.DebugMacros._
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.scala.typeutils.CaseClassSerializer

case class P(a: Integer, b: Int)

class ScalaTestCase {

  @Test
  def test(): Unit = {
    val c = new TestClass
    c.test(classOf[String])
    debug(createTypeInformation[P])
  }

  @Test
  def test2(): Unit = {
    val c = P(0, 0)
    val b = 1
    ScalaTypeExtractor.createTypeInfo((1, ""))
    val t = new GeneralCaseClassTypeInfo(
      classOf[(Int, String)],
      Array(),
      List(
      BasicTypeInfo.getInfoFor(classOf[Int]),
      BasicTypeInfo.getInfoFor(classOf[String])),
      Seq("_1", "_2")
    )
    val t2 = createTypeInformation[(Int, String)]
    val bc: (Int, String) =
      t.createSerializer(new ExecutionConfig).asInstanceOf[CaseClassSerializer[Product]].createInstance().asInstanceOf[(Int, String)]
    println(t)
  }
}
