package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class SymbolTest extends DtsFunSuite {

  test("dts") {
    """
      |export const sym: symbol
      |""".check()
  }

}

object SymbolTest {

  @JSExportTopLevel("sym")
  val sym: js.Symbol = ???
}








