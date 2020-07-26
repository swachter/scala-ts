import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AdapterNestedClassTest extends DtsFunSuite {

  """
    |export interface AdapterNestedClassTest$ {
    |  readonly x: AdapterNestedClassTest.x$
    |  'AdapterNestedClassTest$': never
    |}
    |export const AdapterNestedClassTest: AdapterNestedClassTest$
    |export namespace AdapterNestedClassTest {
    |  interface InstanceAdapter<D> {
    |    readonly $delegate: D
    |    'AdapterNestedClassTest.InstanceAdapter': never
    |  }
    |  interface x$ {
    |    readonly Outer: AdapterNestedClassTest.x.Outer$
    |    'AdapterNestedClassTest.x$': never
    |  }
    |  namespace x {
    |    interface Outer extends AdapterNestedClassTest.InstanceAdapter<x.Outer> {
    |      outerMethod(): number
    |      readonly Inner: AdapterNestedClassTest.x.Outer.Inner$
    |      'AdapterNestedClassTest.x.Outer': never
    |    }
    |    interface Outer$ {
    |      newInstance(x: number): x.Outer
    |      newAdapter(d: x.Outer): AdapterNestedClassTest.x.Outer
    |      'AdapterNestedClassTest.x.Outer$': never
    |    }
    |    namespace Outer {
    |      interface Inner extends AdapterNestedClassTest.InstanceAdapter<any> {
    |        innerMethod(): string
    |        'AdapterNestedClassTest.x.Outer.Inner': never
    |      }
    |      interface Inner$ {
    |        newInstance(y: string): any
    |        newAdapter(d: any): AdapterNestedClassTest.x.Inner
    |        'AdapterNestedClassTest.x.Outer.Inner$': never
    |      }
    |    }
    |  }
    |}
    |export namespace x {
    |  interface Outer {
    |    'x.Outer': never
    |  }
    |  namespace Outer {
    |    interface Inner {
    |      'x.Outer.Inner': never
    |    }
    |  }
    |}
    |""".check()

}

package x {
  case class Outer(val x: Int) {
    def outerMethod() = 2 * x
    case class Inner(val y: String) {
      def innerMethod() = y.reverse
    }
  }
}

@JSExportTopLevel("AdapterNestedClassTest")
object AdapterNestedClassTest extends js.Object {
  @JSExportAll
  trait InstanceAdapter[D] {
    val $delegate: D
  }

  object x extends js.Object {

    object Outer extends js.Object {
      def newInstance(x: Int) = new _root_.x.Outer(x)
      def newAdapter(d: _root_.x.Outer): Outer = new Outer {
        override val $delegate = d
      }
    }
    @JSExportAll
    trait Outer extends InstanceAdapter[_root_.x.Outer] {
      def outerMethod() = $delegate.outerMethod()
      object Inner extends js.Object {
        def newInstance(y: String) = new $delegate.Inner(y)
        def newAdapter(d: $delegate.Inner): Inner = new Inner {
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
