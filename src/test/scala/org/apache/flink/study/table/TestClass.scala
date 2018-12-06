package org.apache.flink.study.table

import org.apache.flink.api.common.typeinfo.TypeInformation

class TestClass {
  def test[T: TypeInformation](cls: Class[T]): Unit = {
    println("with typeinfo: " + cls.toString + ", typeinfo: " + implicitly[TypeInformation[T]].toString)
  }

  def test(cls: Class[_]): Unit = {
    println("with out typeinfo: " + cls.toString)
  }
}
