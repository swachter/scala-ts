package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class SymbolTest extends DtsFunSuite {

  """
    |export const sym: symbol
    |""".check()

}

object SymbolTest {

  @JSExportTopLevel("sym")
  val sym: js.Symbol = ???
}








