package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class SealedTraitAllMembersExportedTest extends DtsFunSuite {

  """
    |export interface Case1 extends dts.SealedTraitAllMembersExportedTest.Base {
    |  'Case1': never
    |}
    |export class Case1 {
    |  constructor()
    |}
    |export interface Case2 extends dts.SealedTraitAllMembersExportedTest.Sub {
    |  'Case2': never
    |}
    |export class Case2 {
    |  constructor()
    |}
    |export namespace dts {
    |  namespace SealedTraitAllMembersExportedTest {
    |    interface Base {
    |      'dts.SealedTraitAllMembersExportedTest.Base': never
    |    }
    |    type Base$u = Case1 | dts.SealedTraitAllMembersExportedTest.Sub$u
    |    interface Sub extends dts.SealedTraitAllMembersExportedTest.Base {
    |      'dts.SealedTraitAllMembersExportedTest.Sub': never
    |    }
    |    type Sub$u = Case2
    |  }
    |}
    |""".check()

}

object SealedTraitAllMembersExportedTest {

  sealed trait Base
  sealed trait Sub extends Base

  @JSExportTopLevel("Case1")
  class Case1 extends Base

  @JSExportTopLevel("Case2")
  class Case2 extends Sub
}










