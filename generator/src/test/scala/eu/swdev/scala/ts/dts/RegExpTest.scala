package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class RegExpTest extends DtsFunSuite {

  """
    |export const regExp: RegExp
    |""".check()

}

object RegExpTest {
  @JSExportTopLevel("regExp")
  val regExp: js.RegExp = ???
}








