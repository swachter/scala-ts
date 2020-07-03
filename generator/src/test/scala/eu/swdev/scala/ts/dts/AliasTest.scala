package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|

class AliasTest extends DtsFunSuite {

  """
    |export function invert(v: dts.AliasTest$.U): dts.AliasTest$.U
    |export namespace dts {
    |  namespace AliasTest$ {
    |    type U = number | string | boolean
    |  }
    |}
    |""".check()

}

object AliasTest {

  type U = Int | String | Boolean

  @JSExportTopLevel("invert")
  def invert(v: U): U = ???
}






