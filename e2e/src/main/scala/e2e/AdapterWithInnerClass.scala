import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import eu.swdev.scala.ts.adapter._

package x {
  package y {
    case class Outer(val x: Int) {
      def outerMethod() = 2 * x
      case class Inner(val y: String) {
        def innerMethod() = y.reverse
      }
    }
  }
}

@JSExportTopLevel("AdapterWithInnerClass")
object AdapterWithInnerClass extends js.Object {
  @JSExportAll
  trait InstanceAdapter[D] {
    val $delegate: D
  }

  object x extends js.Object {

    object y extends js.Object {
      object Outer extends js.Object {
        def newInstance(x: Int) = new _root_.x.y.Outer(x)
        def newAdapter(d: _root_.x.y.Outer) = new Outer {
          override val $delegate = d
        }
      }
      @JSExportAll
      trait Outer extends InstanceAdapter[_root_.x.y.Outer] {
        def outerMethod() = $delegate.outerMethod()
        object Inner {
          def newInstance(y: String) = new $delegate.Inner(y)
          def newAdapter(d: $delegate.Inner) = new Inner {
            override val $delegate = d
          }

        }
        @JSExportAll
        trait Inner extends InstanceAdapter[$delegate.Inner] {
          def innerMethod() = $delegate.innerMethod()
        }
      }
    }

  }

}
