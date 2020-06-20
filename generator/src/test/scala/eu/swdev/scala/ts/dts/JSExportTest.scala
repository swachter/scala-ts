package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

class JSExportTest extends DtsFunSuite {

  test("jsExport") {
    """
      |export interface jsExportTest$ {
      |  readonly x: number
      |  readonly y: number
      |  'jsExportTest$': never
      |}
      |export const jsExportTest: jsExportTest$
      |""".check()
  }

}

@JSExportTopLevel("jsExportTest")
object JSExportTest {

  @JSExport
  val x  = 0
  @JSExport("y")
  val z  = 0

}





