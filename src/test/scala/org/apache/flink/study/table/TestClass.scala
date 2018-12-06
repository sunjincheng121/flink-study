package org.apache.flink.study.table

import org.apache.flink.api.common.typeinfo.TypeInformation

class TestClass {

  /**
    * Scala method call: two method with same name and same signature, one of them has a implicit value
    * and another doesn't have. In this situation, that one without implicit value must not be a generic
    * method. If both of them are generic methods, an compile error will occur.
    */
  def test[T: TypeInformation](cls: Class[T]): Unit = {
    println("with typeinfo: " + cls.toString + ", typeinfo: " + implicitly[TypeInformation[T]].toString)
  }

  def test(cls: Class[_]): Unit = {
    println("with out typeinfo: " + cls.toString)
  }
}
