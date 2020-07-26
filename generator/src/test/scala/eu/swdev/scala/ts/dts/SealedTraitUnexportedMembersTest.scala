package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class SealedTraitUnexportedMembersTest extends DtsFunSuite {

  """
    |export interface Case1 extends dts.SealedTraitUnexportedMembersTest.Base {
    |  'Case1': never
    |}
    |export class Case1 {
    |  constructor()
    |}
    |export namespace dts {
    |  namespace SealedTraitUnexportedMembersTest {
    |    interface Base {
    |      'dts.SealedTraitUnexportedMembersTest.Base': never
    |    }
    |    type Base$u = Case1 | dts.SealedTraitUnexportedMembersTest.Case2 | dts.SealedTraitUnexportedMembersTest.Case3$
    |    interface Case2 extends dts.SealedTraitUnexportedMembersTest.Base {
    |      'dts.SealedTraitUnexportedMembersTest.Case2': never
    |    }
    |    interface Case3$ extends dts.SealedTraitUnexportedMembersTest.Base {
    |      'dts.SealedTraitUnexportedMembersTest.Case3$': never
    |    }
    |  }
    |}
    |""".check()

}

object SealedTraitUnexportedMembersTest {

  sealed trait Base

  @JSExportTopLevel("Case1")
  class Case1 extends Base

  class Case2 extends Base

  object Case3 extends Base
}














