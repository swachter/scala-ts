import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportStatic, JSExportTopLevel}
import scala.util.{Failure, Random, Success}
import js.JSConverters._
import scala.collection.mutable
import scala.concurrent.Promise
import scala.scalajs.js.{Dictionary, UndefOr}

object TopLevelDefsAndVals {

  @JSExportTopLevel("greet")
  def greet(who: String): Unit = println(s"Greetings to $who")

  @JSExportTopLevel("random")
  def random(): Double = Random.nextDouble()

  @JSExportTopLevel("maxLong")
  val maxLong: Long = Long.MaxValue

  @JSExportTopLevel("maxInt")
  val maxInt: Int = Int.MaxValue

  @JSExportTopLevel("globalVar")
  var globalVar = 7

  val twice: Int => Int = _ * 2

  @JSExportTopLevel("twice")
  val twiceJs: js.Function1[Int, Int] = twice

  @JSExportTopLevel("multiParamLists1")
  def multiParamLists1(a: Int)(b: Int, c: Int): Int = a + b + c

  @JSExportTopLevel("multiParamLists2")
  def multiParamLists2(a: Int)(b: Int, c: Int)(d: Int): Int = a + b + c + d

}

@JSExportTopLevel("CaseClass")
@JSExportAll
case class CaseClass(strVal: String)

@JSExportTopLevel("StdClass")
@JSExportAll
class StdClass(var strVar: String) {
  // accessors (get / set) are exported
  var int                   = 5
  def upperProperty: String = strVar.toUpperCase
  def upperMethod(): String = strVar.toUpperCase
}

@JSExportTopLevel("StdClass2")
class StdClass2() {

  var _value   = 5
  var _numbers = Array.fill(5)(0.0)
  var _option  = Option(5.0)
  var _tuple   = ("abc", 1.0)
  var _matrix  = Array.fill(0)(Array.fill(0)(0))

  // export setter and getter for that value explicitly
  @JSExport
  def value_=(v: Int) = _value = v
  @JSExport
  def value = _value

  // export setter and getter for the numbers array
  // that converting between Scala and JavaScript arrays
  @JSExport
  def numbers_=(v: js.Array[Double]): Unit = _numbers = v.toArray

  @JSExport
  def numbers: js.Array[Double] = _numbers.toJSArray

  @JSExport
  def option_=(v: js.UndefOr[Double]): Unit = _option = v.toOption

  @JSExport
  def option: js.UndefOr[Double] = _option.orUndefined

  @JSExport
  def tuple_=(v: js.Array[js.Any]): Unit =
    _tuple = (v(0).asInstanceOf[String], v(1).asInstanceOf[Double])

  @JSExport
  def tuple: js.Array[js.Any] =
    Array(_tuple._1.asInstanceOf[js.Any], _tuple._2.asInstanceOf[js.Any]).toJSArray

  @JSExport
  def matrix_=(v: js.Array[js.Array[Int]]): Unit = _matrix = v.toArray.map(_.toArray)

  @JSExport
  def matrix: js.Array[js.Array[Int]] = _matrix.toJSArray.map(_.toJSArray)

}

@JSExportTopLevel("ArrayAccess")
@JSExportAll
class ArrayAccess(v: js.Array[Int], m: js.Array[js.Array[Int]]) {

  private var _vector = v.toArray
  private var _matrix = m.toArray.map(_.toArray)

  def vector_=(v: js.Array[Int]): Unit = _vector = v.toArray

  def vector: js.Array[Int] = _vector.toJSArray

  def matrix_=(v: js.Array[js.Array[Int]]): Unit = _matrix = v.toArray.map(_.toArray)

  def matrix: js.Array[js.Array[Int]] = _matrix.toJSArray.map(_.toJSArray)

}

@JSExportTopLevel("JsClass")
class JsClass(var int: Int) extends js.Object {}

object JsClass {
  @JSExportStatic
  val staticVal = 5
  @JSExportStatic
  var staticVar = 6
  @JSExportStatic
  def staticDef(n: Int) = n * n
}

@JSExportTopLevel("PromiseInterop")
object PromiseInterop {

  import scala.concurrent.ExecutionContext.Implicits.global

  @JSExport
  def sleepMillis(millis: Double): js.Promise[Unit] = {
    val p = Promise[Unit]
    js.timers.setTimeout(millis)(p.success(()))
    p.future.toJSPromise
  }

  @JSExport
  def onSuccess[T](promise: js.Promise[T], func: js.Function1[T, Unit]): Unit =
    promise.toFuture.onComplete {
      case Success(t) => func(t)
      case Failure(e) => println(e)
    }

}

@JSExportTopLevel("ConstrClass")
// instanceof test works only if js.Object is extended
class ConstrClass(val str: String = "xxx") extends js.Object

@JSExportTopLevel("ConstrClass2")
// instanceof test works only if js.Object is extended
class ConstrClass2(val str: String = "xxx") extends js.Object

@JSExportTopLevel("ClassWithStatics")
class ClassWithStatics extends js.Object

object ClassWithStatics {
  @JSExportStatic
  val constant = 555
  @JSExportStatic
  def method(): Unit = println("static method called")
}

sealed trait Result[+L, +R] extends js.Object {
  def isLeft: Boolean
  def isRight: Boolean
  def left: L
  def right: R
  def tpe: String
}

object Result {
  @JSExportTopLevel("Left")
  class Left[L](val left: L) extends Result[L, Nothing] {
    override def isLeft: Boolean  = true
    override def isRight: Boolean = false
    override def right: Nothing   = ???
    override def tpe: "Left"      = "Left"
  }
  @JSExportTopLevel("Right")
  class Right[R](val right: R) extends Result[Nothing, R] {
    override def isLeft: Boolean  = false
    override def isRight: Boolean = true
    override def left: Nothing    = ???
    override def tpe: "Right"     = "Right"
  }
}

@JSExportTopLevel("stdLibInterOp")
@JSExportAll
object StdLibInterop {

  def toOption[T](t: UndefOr[T]): Option[T] = t.toOption

  def fromOption[T](t: Option[T]): UndefOr[T] = t.orUndefined

  def toSome[T](t: T): Some[T] = Some(t)

  val none: None.type = None

  def asMap[V](d: Dictionary[V]): mutable.Map[String, V]             = d
  def addToMap[K, V](key: K, value: V, map: mutable.Map[K, V]): Unit = map(key) = value
  def toDictionary[V](map: collection.Map[String, V]): Dictionary[V] = map.toJSDictionary

  def list[V](vs: V*): List[V]                = List(vs: _*)
  def noneEmptyList[V](v: V, vs: V*): List[V] = v :: List(vs: _*)

  def immutableMap[K, V](kvs: scala.scalajs.js.Tuple2[K, V]*): Map[K, V] =
    Map(kvs.map(kv => kv._1 -> kv._2): _*)
}

@JSExportTopLevel("A")
@JSExportAll
class A(val n: Int)
@JSExportTopLevel("B")
@JSExportAll
class B(n: Int, val s: String) extends A(n)
