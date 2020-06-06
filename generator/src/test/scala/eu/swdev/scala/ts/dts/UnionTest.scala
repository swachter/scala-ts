package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|

class UnionTest extends DtsFunSuite {

  test("dts") {
    """
      |export const stringOrBooleanOrInt: string | boolean | number
      |""".check()
  }

}

object UnionTest {

  @JSExportTopLevel("stringOrBooleanOrInt")
  val x: String | Boolean | Int = ???

}




