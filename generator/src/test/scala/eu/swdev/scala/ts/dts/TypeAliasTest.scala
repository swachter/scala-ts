package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|

class TypeAliasTest extends DtsFunSuite {

  test("dts") {
    """
      |export const valWithTypeAlias: eu.swdev.scala.ts.dts.TypeAliasTest.X
      |export namespace eu {
      |    namespace swdev {
      |        namespace scala {
      |            namespace ts {
      |                namespace dts {
      |                    namespace TypeAliasTest {
      |                        type X = string | number
      |                    }
      |                }
      |            }
      |        }
      |    }
      |}
      |""".check()
  }

}

object TypeAliasTest {

  type X = String | Int

  @JSExportTopLevel("valWithTypeAlias")
  val alias: X = ???
}




