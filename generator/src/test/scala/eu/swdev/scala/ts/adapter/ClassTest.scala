package test

import eu.swdev.scala.ts.AdapterFunSuite
import eu.swdev.scala.ts.annotation.AdaptAll

class ClassTest extends AdapterFunSuite {
  """
    |import scala.scalajs.js
    |import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
    |import eu.swdev.scala.ts.adapter._
    |@JSExportTopLevel("Adapter")
    |object Adapter extends js.Object {
    |  @JSExportAll
    |  trait InstanceAdapter[D] {
    |    val $delegate: D
    |  }
    |  object test extends js.Object {
    |    @JSExportAll
    |    trait AdaptedClass extends InstanceAdapter[_root_.test.AdaptedClass] {
    |      def x = $res($delegate.x)
    |      def sum = $res($delegate.sum)
    |    }
    |    object AdaptedClass extends js.Object {
    |      def newAdapter(delegate: _root_.test.AdaptedClass): AdaptedClass = new AdaptedClass {
    |        $delegate = delegate
    |      }
    |      def newInstance(x: Int, y: Int) = new _root_.test.AdaptedClass(x.$cnv[Int], y.$cnv[Int])
    |    }
    |  }
    |}
    |""".check()
}

@AdaptAll
class AdaptedClass(val x: Int, y: Int) {

  def sum = x + y
}