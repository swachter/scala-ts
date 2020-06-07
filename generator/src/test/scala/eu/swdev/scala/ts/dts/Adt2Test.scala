package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.JSExportTopLevel

class Adt2Test extends DtsFunSuite {

  test("simple") {
    """
      |export interface Adt2Case1<Y> extends eu.swdev.scala.ts.dts.Adt2Test.Base<Y> {
      |  'Adt2Case1': never
      |}
      |export class Adt2Case1<Y> {
      |  constructor()
      |}
      |export interface Adt2Case2 extends eu.swdev.scala.ts.dts.Adt2Test.Base<never> {
      |  'Adt2Case2': never
      |}
      |export class Adt2Case2 {
      |  constructor()
      |}
      |export namespace eu {
      |    namespace swdev {
      |        namespace scala {
      |            namespace ts {
      |                namespace dts {
      |                    namespace Adt2Test {
      |                        interface Base<X> {
      |                          'eu.swdev.scala.ts.dts.Adt2Test.Base': never
      |                        }
      |                        type Base$<X> = Adt2Case1<X> | Adt2Case2
      |                    }
      |                }
      |            }
      |        }
      |    }
      |}
      |""".check()
  }
}

object Adt2Test {

  sealed trait Base[X]

  @JSExportTopLevel("Adt2Case1")
  class Case1[Y] extends Base[Y]

  @JSExportTopLevel("Adt2Case2")
  class Case2 extends Base[Nothing]

}

