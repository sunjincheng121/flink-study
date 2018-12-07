package org.apache.flink.study.table

import org.apache.commons.lang3.reflect.MethodUtils
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.java.typeutils.runtime.TupleSerializerBase
import org.apache.flink.api.scala.typeutils.{CaseClassSerializer, CaseClassTypeInfo}


class GeneralCaseClassTypeInfo(
    cls: Class[_ <: Product],
    typeParams: Array[TypeInformation[_]],
    typeinfos: List[TypeInformation[_]],
    names: Seq[String])
  extends CaseClassTypeInfo[Product](cls.asInstanceOf[Class[Product]], typeParams, typeinfos, names) {

  override def createSerializer(executionConfig: ExecutionConfig): TypeSerializer[Product] = {

    val fieldSerializers: Array[TypeSerializer[_]] = new Array[TypeSerializer[_]](getArity)

    for (i <- 0 until getArity ) {
      fieldSerializers.update(i, types(i).createSerializer(executionConfig))
    }

    class GeneralCaseClassSerializer
      extends CaseClassSerializer[Product](getTypeClass, fieldSerializers) {

      override def createInstance(objects: Array[AnyRef]): Product = {
        val seq: Array[Class[_]] = objects.map(_.getClass.asInstanceOf[Class[_]])
        val method =
          MethodUtils.getMatchingAccessibleMethod(tupleClass, "apply", seq: _*)

        val newObjects: Array[Object] = objects
        method.invoke(null, newObjects: _*).asInstanceOf[Product]
      }

      override def createSerializerInstance(
          tupleClass: Class[Product],
          fieldSerializers: Array[TypeSerializer[_]]): TupleSerializerBase[Product] = {
        this.getClass.getConstructors()(0)
          .newInstance(tupleClass, fieldSerializers).asInstanceOf[CaseClassSerializer[Product]]
      }
    }

    new GeneralCaseClassSerializer
  }
}
