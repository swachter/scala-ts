import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AdapterClassTest extends DtsFunSuite {
  """
    |export interface AdapterClassTest$ {
    |  readonly x: AdapterClassTest.x$
    |  'AdapterClassTest$': never
    |}
    |export const AdapterClassTest: AdapterClassTest$
    |export namespace AdapterClassTest {
    |  interface InstanceAdapter<D> {
    |    readonly $delegate: D
    |    'AdapterClassTest.InstanceAdapter': never
    |  }
    |  interface x$ {
    |    readonly SomeClass: AdapterClassTest.x.SomeClass$
    |    'AdapterClassTest.x$': never
    |  }
    |  namespace x {
    |    interface SomeClass extends AdapterClassTest.InstanceAdapter<x.SomeClass> {
    |      readonly array: number[]
    |      readonly sum: number
    |      'AdapterClassTest.x.SomeClass': never
    |    }
    |    interface SomeClass$ {
    |      newInstance(array: number[]): x.SomeClass
    |      newAdapter(d: x.SomeClass): AdapterClassTest.x.SomeClass
    |      'AdapterClassTest.x.SomeClass$': never
    |    }
    |  }
    |}
    |export namespace x {
    |  interface SomeClass {
    |    'x.SomeClass': never
    |  }
    |}
    |""".check()
}

package x {
  class SomeClass(val array: Array[Int]) {
    def sum: Int = ???
  }
}

@JSExportTopLevel("AdapterClassTest")
object AdapterClassTest extends js.Object {

  @JSExportAll
  trait InstanceAdapter[D] {
    val $delegate: D
  }

  object x extends js.Object {
    @JSExportAll
    trait SomeClass extends InstanceAdapter[_root_.x.SomeClass] {
      def array: js.Array[Int] = ???
      def sum: Int             = ???
    }
    object SomeClass extends js.Object {
      def newInstance(array: js.Array[Int]): _root_.x.SomeClass = ???
      def newAdapter(d: _root_.x.SomeClass): SomeClass = new SomeClass {
        override val $delegate = d
      }
    }
  }

}
