package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class DateTest extends DtsFunSuite {

  """
    |export const date: Date
    |""".check()

}

object DateTest {
  @JSExportTopLevel("date")
  val date: js.Date = ???
}
