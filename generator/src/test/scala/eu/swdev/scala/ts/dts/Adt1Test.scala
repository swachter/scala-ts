package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.JSExportTopLevel

class Adt1Test extends DtsFunSuite {

  test("simple") {
    """
      |export interface Adt1Case1 extends eu.swdev.scala.ts.dts.Adt1Test.Base {
      |  'eu.swdev.scala.ts.dts.Adt1Test.Case1': never
      |}
      |export class Adt1Case1 {
      |  constructor()
      |}
      |export interface Adt1Case2 extends eu.swdev.scala.ts.dts.Adt1Test.Base {
      |  'eu.swdev.scala.ts.dts.Adt1Test.Case2': never
      |}
      |export class Adt1Case2 {
      |  constructor()
      |}
      |export namespace eu {
      |    namespace swdev {
      |        namespace scala {
      |            namespace ts {
      |                namespace dts {
      |                    namespace Adt1Test {
      |                        interface Base {
      |                          'eu.swdev.scala.ts.dts.Adt1Test.Base': never
      |                        }
      |                        type Base$ = Adt1Case1 | Adt1Case2
      |                    }
      |                }
      |            }
      |        }
      |    }
      |}
      |""".check()
  }
}

object Adt1Test {

  sealed trait Base

  @JSExportTopLevel("Adt1Case1")
  class Case1 extends Base

  @JSExportTopLevel("Adt1Case2")
  class Case2 extends Base

}
