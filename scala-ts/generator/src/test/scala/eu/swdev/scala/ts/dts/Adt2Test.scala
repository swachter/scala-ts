package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.JSExportTopLevel

class Adt2Test extends DtsFunSuite {

  test("simple") {
    """
      |export class Adt1Case1<Y> {
      |  constructor()
      |}
      |export class Adt1Case2 {
      |  constructor()
      |}
      |export namespace eu {
      |    namespace swdev {
      |        namespace scala {
      |            namespace ts {
      |                namespace dts {
      |                    namespace Adt2Test {
      |                        type Base<X> = Adt2Case1<X> | Adt2Case2
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

