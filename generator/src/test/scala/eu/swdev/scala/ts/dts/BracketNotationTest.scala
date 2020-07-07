package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSName}

class BracketNotationTest extends DtsFunSuite {

  """
    |export interface BracketNotation$ {
    |  readonly ['!x']: number
    |  ['!y']: number
    |  ['!z'](): number
    |  'BracketNotation$': never
    |}
    |export const BracketNotation: BracketNotation$
    |""".check()

}

@JSExportTopLevel("BracketNotation")
object BracketNotationTest extends js.Object {

  @JSName("!x")
  val x = 1

  @JSName("!y")
  var y = 1

  @JSName("!z")
  def z() = 1

}
