package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class SealedTraitSomeMembersExportedTest extends DtsFunSuite {

  """
    |export interface Case1 extends dts.SealedTraitSomeMembersExportedTest.Base {
    |  'Case1': never
    |}
    |export class Case1 {
    |  constructor()
    |}
    |export interface Case2 extends dts.SealedTraitSomeMembersExportedTest.Sub {
    |  'Case2': never
    |}
    |export class Case2 {
    |  constructor()
    |}
    |export namespace dts {
    |  namespace SealedTraitSomeMembersExportedTest {
    |    interface Base {
    |      'dts.SealedTraitSomeMembersExportedTest.Base': never
    |    }
    |    type Base$u = Case1 | dts.SealedTraitSomeMembersExportedTest.Sub$u
    |    interface Case3 extends dts.SealedTraitSomeMembersExportedTest.Sub {
    |      'dts.SealedTraitSomeMembersExportedTest.Case3': never
    |    }
    |    interface Sub extends dts.SealedTraitSomeMembersExportedTest.Base {
    |      'dts.SealedTraitSomeMembersExportedTest.Sub': never
    |    }
    |    type Sub$u = Case2 | dts.SealedTraitSomeMembersExportedTest.Case3
    |  }
    |}
    |""".check()

}

object SealedTraitSomeMembersExportedTest {
  sealed trait Base
  sealed trait Sub extends Base

  @JSExportTopLevel("Case1")
  class Case1 extends Base

  @JSExportTopLevel("Case2")
  class Case2 extends Sub

  class Case3 extends Sub

}











