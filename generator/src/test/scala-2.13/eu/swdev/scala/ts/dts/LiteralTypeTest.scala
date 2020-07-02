package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class LiteralTypeTest extends DtsFunSuite {

  """
    |export const litString: 'a'
    |export const litNumber: 1
    |export const litBoolean: true
    |""".check()

}

object LiteralTypeTest {

  @JSExportTopLevel("litString")
  val litString: "a" = "a"
  @JSExportTopLevel("litNumber")
  val litNumber: 1 = 1
  @JSExportTopLevel("litBoolean")
  val litBoolean: true = true

}
