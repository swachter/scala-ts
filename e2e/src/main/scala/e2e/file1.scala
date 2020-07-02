package e2e

import typings.jsJoda.mod.{LocalDate}

import scala.annotation.meta.field
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportStatic, JSExportTopLevel, JSGlobal}
import scala.scalajs.js.{ThisFunction1, UndefOr, |}

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
    val str    = "abc"
  }
  @JSExportTopLevel("ObjectAdtCase2")
  @JSExportAll
  object Case2 extends Adt {
    val tpe: 2 = 2
    val num    = 555
  }

}

@JSExportTopLevel("JsClass")
class JsClass(val initialStr: String, var num: Int) extends js.Object {
  private var _str           = initialStr
  def str_=(s: String): Unit = _str = s
  def str                    = _str
  def doubleNum(): Unit      = num = num * 2
}

object FunctionTest {
  @JSExportTopLevel("fun0")
  def fun0[R](f: js.Function0[R]): R = f()
  @JSExportTopLevel("fun1")
  def fun1[T1, R](a: js.Array[T1], f: js.Function1[T1, js.Array[R]]): js.Array[R] = a.flatMap(f)
  @JSExportTopLevel("fun2")
  def fun2[T1, T2, R](a1: js.Array[T1], a2: js.Array[T2], f: js.Function2[T1, T2, R]): js.Array[R] = a1.zip(a2).map(f.tupled)
}

object TupleTest {
  @JSExportTopLevel("tupleFunction")
  def tupleFunction[T1, T2, R](f: js.Function2[T1, T2, R]): js.Function1[js.Tuple2[T1, T2], R] = {
    val x = f.tupled
    t =>
      x(t)
  }
}

object UnionTest {

  type U = Int | String | Boolean

  @JSExportTopLevel("invert")
  def invert(v: U): U =
    if (v.isInstanceOf[Int]) {
      -v.asInstanceOf[Int]
    } else if (v.isInstanceOf[String]) {
      v.asInstanceOf[String].reverse
    } else {
      !v.asInstanceOf[Boolean]
    }
}

object DictionaryTest {

  @JSExportTopLevel("sumDict")
  def sumValues(d: js.Dictionary[Int]) = d.values.sum

  @JSExportTopLevel("addToDict")
  def add[T](k: String, v: T, d: js.Dictionary[T]) = d.addOne(k -> v)
}

object PromiseTest {

  import scala.concurrent.ExecutionContext.Implicits.global

  @JSExportTopLevel("mapPromise")
  def map[A, B](p: js.Promise[A], f: js.Function1[A, B]): js.Promise[B] = {
    p.toFuture.map(f).toJSPromise
  }

}

object DateTest {

  @JSExportTopLevel("fullYearOfDate")
  def fullYearOfDate(d: js.Date): Double = d.getFullYear()
}

object RegExpTest {

  @JSExportTopLevel("regExpMatches")
  def matches(r: js.RegExp, s: String): Boolean = r.test(s)

  @JSExportTopLevel("createRegExp")
  def regexp(s: String): js.RegExp = js.RegExp(s)
}

object VarArgsTest {
  @JSExportTopLevel("sumVarArgs")
  def sum(is: Int*): Int = is.sum
  @JSExportTopLevel("createDictionary")
  def dictionary[V](ts: js.Tuple2[String, V]*): js.Dictionary[V] = js.Dictionary.apply(ts.map(js.Tuple2.toScalaTuple2(_)): _*)
}

object ThisFunctionTest {

  type Listener = ThisFunction1[Unit, String, Unit]

  @JSExportTopLevel("Notifier")
  @JSExportAll
  class Notifier {
    private val listeners              = mutable.ArrayBuffer.empty[Listener]
    def addListener(l: Listener): Unit = listeners += l
    def notify(s: String): Unit        = listeners.foreach(_.apply((), s))
  }

  @JSExportTopLevel("Listener")
  @JSExportAll
  class ListenerImpl {
    var s: String                                         = _
    def notify(s: String)                                 = this.s = s
    def notifyFunction: ThisFunction1[Unit, String, Unit] = (_, s) => notify(s)
  }

}

object IteratorTest {
  @JSExportTopLevel("sumIterable")
  def sum(i: js.Iterable[Int]): Int = i.sum
  @JSExportTopLevel("numberIterator")
  def iterator(n: Int): js.Iterator[Int] = (0 to n).toJSIterable.jsIterator()
}

object ApiReferenceTest {

  // types Input, Output, In1, In2, In2$, Out1, and Out2$ are exported
  // -> they are reference in the exported method

  type In2  = In2.type
  type Out2 = Out2.type // the Out2 type alias is not exported because it is not referenced

  type Input  = In1 | In2
  type Output = Out1 | Out2.type

  @JSExportTopLevel("convertInput2Output")
  def convertInput2Output(i: Input): Output = {
    if (i.isInstanceOf[In1]) {
      new Out1(i.asInstanceOf[In1].i)
    } else {
      Out2
    }
  }

  @JSExportTopLevel("createInput")
  def createInput(i: Int): Input = {
    if (i % 2 == 0) {
      new In1(i)
    } else {
      In2
    }
  }

  class Out1(val i: Int) extends js.Object

  object Out2 extends js.Object {
    val s = "abc"
  }

  class In1(val i: Int) extends js.Object

  object In2 extends js.Object {
    val s = "abc"
  }

}

object GlobalTest {
  @js.native
  @JSGlobal("WeakMap")
  class WeakMap[K <: js.Object, V <: js.Any] extends js.Object {

    def delete(key: K): Unit = js.native

    def has(key: K): Boolean = js.native

    def get(key: K): UndefOr[V] = js.native

    def set(key: K, value: V): Unit = js.native

  }

  @JSExportTopLevel("setInWeakMap")
  def setInWeakMap[K <: js.Object, V <: js.Any](key: K, value: V, map: WeakMap[K, V]): Unit = map.set(key, value)
}

object ImportTest {

  // LocalDate is imported from js-joda
  // -> the Scalably typed plugin generates a corresponding ScalaJS facade

  @JSExportTopLevel("addDays")
  def addDays(ld: LocalDate, days: Int) = ld.plusDays(days)
}

object AbstractTest {

  @JSExportAll
  abstract class Base[X](val x: X)

  @JSExportTopLevel("AbstractTestCase1")
  class Case1(s: String) extends Base(s)
  @JSExportTopLevel("AbstractTestCase2")
  class Case2(i: Int) extends Base(i)

}

object CtorParamExportTest {

  @JSExportTopLevel("CtorParamExport")
  class CtorParamExport(@JSExport val x: String, @JSExport("y")var z: String)
}
