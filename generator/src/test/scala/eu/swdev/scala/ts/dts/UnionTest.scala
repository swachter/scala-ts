package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|

class UnionTest extends DtsFunSuite {

  """
    |export const stringOrBooleanOrInt: string | boolean | number
    |""".check()

}

object UnionTest {

  @JSExportTopLevel("stringOrBooleanOrInt")
  val x: String | Boolean | Int = ???

}




