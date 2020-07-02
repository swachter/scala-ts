package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|

class ApiReferenceTest extends DtsFunSuite {

  """
    |export function someExportedMethod(in: dts.ApiReferenceTest.Input): dts.ApiReferenceTest.Output
    |export namespace dts {
    |  namespace ApiReferenceTest {
    |    interface In1 {
    |      readonly i: number
    |      'dts.ApiReferenceTest.In1': never
    |    }
    |    type In2 = dts.ApiReferenceTest.In2$
    |    interface In2$ {
    |      readonly s: string
    |      'dts.ApiReferenceTest.In2$': never
    |    }
    |    type Input = dts.ApiReferenceTest.In1 | dts.ApiReferenceTest.In2
    |    interface Out1 {
    |      readonly i: number
    |      'dts.ApiReferenceTest.Out1': never
    |    }
    |    interface Out2$ {
    |      readonly s: string
    |      'dts.ApiReferenceTest.Out2$': never
    |    }
    |    type Output = dts.ApiReferenceTest.Out1 | dts.ApiReferenceTest.Out2$
    |  }
    |}
    |""".check()

}

object ApiReferenceTest {

  // types Input, Output, In1, In2, In2$, Out1, and Out2$ are exported
  // -> they are reference in the exported method

  type In2  = In2.type
  type Out2 = Out2.type // the Out2 type alias is not exported because it is not referenced

  type Input  = In1 | In2
  type Output = Out1 | Out2.type

  @JSExportTopLevel("someExportedMethod")
  def someExportedMethod(in: Input): Output = ???

  class Out1(val i: Int) extends js.Object

  object Out2 extends js.Object {
    val s = "abc"
  }

  class In1(val i: Int) extends js.Object

  object In2 extends js.Object {
    val s = "abc"
  }

}
