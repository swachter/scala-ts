package o

import eu.swdev.scala.ts.annotation.AdaptAll

/* adapter:

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import eu.swdev.scala.ts.adapter._
@JSExportAll
trait InstanceAdapter[D] {
  val $delegate: D
}
@JSExportTopLevel("Adapter")
object Adapter extends js.Object {
  object o extends js.Object {
    @JSExportAll
    trait OuterClass extends InstanceAdapter[_root_.o.OuterClass] {
      def x = $delegate.x.$res
      val Inner$a = Inner
      @JSExportAll
      trait Inner extends InstanceAdapter[_root_.o.OuterClass#Inner] {
        def y = $delegate.y.$res
      }
      object nner extends js.Object {
        def newAdapter(delegate: $delegate.Inner): Inner = new Inner {
          override val $delegate = delegate
        }
        def newDelegate(y: String) = new $delegate.Inner(y.$cnv[String])
      }
    }
    object OuterClass extends js.Object {
      def newAdapter(delegate: _root_.o.OuterClass): OuterClass = new OuterClass {
        override val $delegate = delegate
      }
      def newDelegate(x: Int) = new _root_.o.OuterClass(x.$cnv[Int])
    }
  }
}

 */
@AdaptAll
class OuterClass(val x: Int) {
  @AdaptAll
  class Inner(val y: String)
}
