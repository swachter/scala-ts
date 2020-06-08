package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class DateTest extends DtsFunSuite {

  test("dts") {
    """
      |export const date: Date
      |""".check()
  }

}

object DateTest {
  @JSExportTopLevel("date")
  val date: js.Date = ???
}







