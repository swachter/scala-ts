import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import eu.swdev.scala.ts.adapter._

/* .d.ts:

 */
@JSExportTopLevel("Adapter1")
object Adapter1 extends js.Object {
  @JSExportAll
  trait InstanceAdapter[D] {
    val $delegate: D
  }
  object o extends js.Object {
    @JSExportAll
    trait OuterClass extends InstanceAdapter[_root_.o.OuterClass] {
      def x = $delegate.x.$res

      @JSExportAll
      trait Inner extends InstanceAdapter[$delegate.Inner] {
        def y = $delegate.y.$res
      }

      object Inner extends js.Object {
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
