package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class RegExpTest extends DtsFunSuite {

  test("dts") {
    """
      |export const regExp: RegExp
      |""".check()
  }

}

object RegExpTest {
  @JSExportTopLevel("regExp")
  val regExp: js.RegExp = ???
}








