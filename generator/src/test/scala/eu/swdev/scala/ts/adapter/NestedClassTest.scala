package adapter

import eu.swdev.scala.ts.AdapterFunSuite
import eu.swdev.scala.ts.annotation.AdaptAll

class NestedClassTest extends AdapterFunSuite {

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
    |  object adapter extends js.Object {
    |    @JSExportAll
    |    trait OuterClass extends InstanceAdapter[_root_.adapter.OuterClass] {
    |      def x = $delegate.x.$cnv[Int]
    |      @JSExportAll
    |      trait Inner extends InstanceAdapter[_root_.adapter.OuterClass#Inner] {
    |        def y = $delegate.y.$cnv[String]
    |      }
    |      object Inner extends js.Object {
    |        def newAdapter(delegate: _root_.adapter.OuterClass#Inner): Inner = new Inner {
    |          override val $delegate = delegate
    |        }
    |        def newDelegate(y: String): _root_.adapter.OuterClass#Inner = new $delegate.Inner(y.$cnv[String])
    |      }
    |    }
    |    object OuterClass extends js.Object {
    |      def newAdapter(delegate: _root_.adapter.OuterClass): OuterClass = new OuterClass {
    |        override val $delegate = delegate
    |      }
    |      def newDelegate(x: Int): _root_.adapter.OuterClass = new _root_.adapter.OuterClass(x.$cnv[Int])
    |    }
    |  }
    |}
    |""".check()

}

@AdaptAll
class OuterClass(val x: Int) {
  @AdaptAll
  class Inner(val y: String)
}

