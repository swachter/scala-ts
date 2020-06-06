package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import js.JSConverters._

@JSExportTopLevel("Simple")
case class Simple(int: Int, string: String, boolean: Boolean, double: Double)

object Test {

  @JSExportTopLevel("simple")
  def simple(v: Simple): Simple = v

  @JSExportTopLevel("option")
  def option[T](t: T): Option[T] = Some(t)
}

object TopLevelDefValVar {

  @JSExportTopLevel("immutable")
  val immutable = 5
  @JSExportTopLevel("mutable")
  var mutable = "abc"
  @JSExportTopLevel("setMutable")
  def setMutable(s: String): Unit = mutable = s

  @JSExportTopLevel("multiply")
  def multiply(x: Int, y: Int) = x * y
}

@JSExportTopLevel("TypeConversions")
class TypeConversions {

  var _matrix = Array.fill(0)(Array.fill(0)(0))

  @JSExport
  def matrix_=(v: js.Array[js.Array[Int]]): Unit = _matrix = v.toArray.map(_.toArray)

  @JSExport
  def matrix: js.Array[js.Array[Int]] = _matrix.toJSArray.map(_.toJSArray)

}

object SimpleAdt {

  sealed trait Adt

  @JSExportTopLevel("SimpleAdtCase1")
  @JSExportAll
  case class Case1(int: Int) extends Adt {
    val tpe: "i" = "i"
  }
  @JSExportTopLevel("SimpleAdtCase2")
  @JSExportAll
  case class Case2(str: String) extends Adt {
    val tpe: "s" = "s"
  }

}

object ObjectAdt {

  sealed trait Adt

  @JSExportTopLevel("ObjectAdtCase1")
  @JSExportAll
  object Case1 extends Adt {
    val tpe: 1 = 1
    val str = "abc"
  }
  @JSExportTopLevel("ObjectAdtCase2")
  @JSExportAll
  object Case2 extends Adt {
    val tpe: 2 = 2
    val num = 555
  }

}

@JSExportTopLevel("JsClass")
class JsClass(val initialStr: String, var num: Int) extends js.Object {
  private var _str = initialStr
  def str_=(s: String): Unit = _str = s
  def str = _str
  def doubleNum(): Unit = num = num * 2
}

object FunctionTest {
  @JSExportTopLevel("fun0")
  def fun0[R](f: js.Function0[R]): R = f()
  @JSExportTopLevel("fun1")
  def fun1[T1, R](a: js.Array[T1], f: js.Function1[T1, js.Array[R]]): js.Array[R] = a.flatMap(f)
  @JSExportTopLevel("fun2")
  def fun2[T1, T2, R](a1: js.Array[T1], a2: js.Array[T2], f: js.Function2[T1, T2, R]): js.Array[R] = a1.zip(a2).map(f.tupled)
}
