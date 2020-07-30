import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

@JSExportTopLevel("AdapterV1")
object AdapterV1 extends js.Object {

  @JSExportAll
  trait InstanceAdapter[D] {
    val $delegate: D
  }

  object x extends js.Object {

    @JSExportAll
    trait OuterV1 extends InstanceAdapter[_root_.x.OuterV1] {

      def x: Int = $delegate.x

      @JSExportAll
      trait  MiddleV1 extends InstanceAdapter[_root_.x.OuterV1#MiddleV1] {

        def y: String = $delegate.y

        @JSExportAll
        trait  InnerV1 extends InstanceAdapter[_root_.x.OuterV1#MiddleV1#InnerV1] {
          def z = $delegate.z
        }

        object InnerV1 extends js.Object {
          def newInstance(z: Boolean): _root_.x.OuterV1#MiddleV1#InnerV1 = new $delegate.InnerV1(z)
          def newAdapter(d: _root_.x.OuterV1#MiddleV1#InnerV1): InnerV1 = new InnerV1 {
            override val $delegate = d
          }
        }

        val innerV1 = InnerV1

      }

      object MiddleV1 extends js.Object {
        def newInstance(y: String): _root_.x.OuterV1#MiddleV1 = new $delegate.MiddleV1(y)
        def newAdapter(d: _root_.x.OuterV1#MiddleV1): MiddleV1 = new MiddleV1 {
          override val $delegate = d
          override def y = $delegate.y
        }
      }

      val middleV1 = MiddleV1

    }

    object OuterV1 extends js.Object {
      def newInstance(x: Int) = new _root_.x.OuterV1(x)
      def newAdapter(d: _root_.x.OuterV1): OuterV1 = new OuterV1 {
        override val $delegate = d
        override def x = $delegate.x
      }
    }

    val outerV1 = OuterV1

  }
}

package x {

  class OuterV1(val x: Int) {
    class MiddleV1(val y: String) {
      class InnerV1(val z: Boolean) {

      }
    }
  }
}