import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

class AdapterV1Test extends DtsFunSuite {
  """
    |export interface AdapterV1$ {
    |  readonly x: _root_AdapterV1.x$
    |  'AdapterV1$': never
    |}
    |export const AdapterV1: AdapterV1$
    |export namespace AdapterV1 {
    |  interface InstanceAdapter<D> {
    |    readonly $delegate: D
    |    'AdapterV1.InstanceAdapter': never
    |  }
    |  interface x$ {
    |    readonly OuterV1: _root_AdapterV1.x.OuterV1$
    |    readonly outerV1: AdapterV1.x.OuterV1
    |    'AdapterV1.x$': never
    |  }
    |  namespace x {
    |    interface OuterV1 extends AdapterV1.InstanceAdapter<_root_x.OuterV1> {
    |      readonly x: number
    |      readonly MiddleV1: _root_AdapterV1.x.OuterV1.MiddleV1$
    |      readonly middleV1: _root_AdapterV1.x.OuterV1.MiddleV1$
    |      'AdapterV1.x.OuterV1': never
    |    }
    |    interface OuterV1$ {
    |      newInstance(x: number): _root_x.OuterV1
    |      newAdapter(d: _root_x.OuterV1): AdapterV1.x.OuterV1
    |      'AdapterV1.x.OuterV1$': never
    |    }
    |    namespace OuterV1 {
    |      interface MiddleV1 extends AdapterV1.InstanceAdapter<_root_x.OuterV1.MiddleV1> {
    |        readonly y: string
    |        readonly InnerV1: _root_AdapterV1.x.OuterV1.MiddleV1.InnerV1$
    |        readonly innerV1: _root_AdapterV1.x.OuterV1.MiddleV1.InnerV1$
    |        'AdapterV1.x.OuterV1.MiddleV1': never
    |      }
    |      interface MiddleV1$ {
    |        newInstance(y: string): _root_x.OuterV1.MiddleV1
    |        newAdapter(d: _root_x.OuterV1.MiddleV1): _root_AdapterV1.x.OuterV1.MiddleV1
    |        'AdapterV1.x.OuterV1.MiddleV1$': never
    |      }
    |      namespace MiddleV1 {
    |        interface InnerV1 extends AdapterV1.InstanceAdapter<_root_x.OuterV1.MiddleV1.InnerV1> {
    |          readonly z: boolean
    |          'AdapterV1.x.OuterV1.MiddleV1.InnerV1': never
    |        }
    |        interface InnerV1$ {
    |          newInstance(z: boolean): _root_x.OuterV1.MiddleV1.InnerV1
    |          newAdapter(d: _root_x.OuterV1.MiddleV1.InnerV1): _root_AdapterV1.x.OuterV1.MiddleV1.InnerV1
    |          'AdapterV1.x.OuterV1.MiddleV1.InnerV1$': never
    |        }
    |      }
    |    }
    |  }
    |}
    |export namespace x {
    |  interface OuterV1 {
    |    'x.OuterV1': never
    |  }
    |  namespace OuterV1 {
    |    interface MiddleV1 {
    |      'x.OuterV1.MiddleV1': never
    |    }
    |    namespace MiddleV1 {
    |      interface InnerV1 {
    |        'x.OuterV1.MiddleV1.InnerV1': never
    |      }
    |    }
    |  }
    |}
    |import _root_AdapterV1 = AdapterV1
    |import _root_x = x
    |""".check(addRootNamespace = true)
}

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