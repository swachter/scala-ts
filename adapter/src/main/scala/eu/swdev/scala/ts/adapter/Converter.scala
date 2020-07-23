package eu.swdev.scala.ts.adapter

import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

trait Converter[F, +T] {
  def apply(f: F): T
}

trait Prio3Converter {

  implicit def idConverter[X]: Converter[X, X] = identity
}

trait Prio2Converter extends Prio3Converter {
  implicit def jsArray2ScalaArray[X: ClassTag, Y: ClassTag](implicit ev: Converter[X, Y]): Converter[js.Array[X], Array[Y]] = _.toArray.map(ev(_))

}

object Converter extends Prio2Converter {

  def apply[X, Y](implicit ev: Converter[X, Y]): Converter[X, Y] = ev

  implicit def jsArray2ScalaList[X, Y](implicit ev: Converter[X, Y]): Converter[js.Array[X], List[Y]] = _.toList.map(ev(_))
  implicit def scalaArray2JsArray[X, Y](implicit ev: Converter[X, Y]): Converter[Array[X], js.Array[Y]] = _.toJSArray.map(ev(_))

  implicit def undefOr2Option[X, Y](implicit ev: Converter[X, Y]): Converter[js.UndefOr[X], Option[Y]] = _.toOption.map(ev(_))
  implicit def option2undefOr[X, Y](implicit ev: Converter[X, Y]): Converter[Option[X], js.UndefOr[Y]] = _.orUndefined.map(ev(_))

  implicit def scalaList2JsArray[X: ClassTag, Y: ClassTag](implicit ev: Converter[X, Y]): Converter[List[X], js.Array[Y]] = _.toJSArray.map(ev(_))

}

trait JavaScriptType[F, T]

trait LowPrioJavaScriptTypes {
  implicit def id[X]: JavaScriptType[X, X] = null
}

object JavaScriptType extends LowPrioJavaScriptTypes {
  implicit def array[X, Y](implicit ev: JavaScriptType[X, Y]): JavaScriptType[Array[X], js.Array[Y]] = null
  implicit def list[X, Y](implicit ev: JavaScriptType[X, Y]): JavaScriptType[List[X], js.Array[Y]] = null
  implicit def option[X, Y](implicit ev: JavaScriptType[X, Y]): JavaScriptType[Option[X], js.UndefOr[Y]] = null
}
