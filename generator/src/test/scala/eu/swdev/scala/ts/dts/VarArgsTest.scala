package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.JSExportTopLevel

class VarArgsTest extends DtsFunSuite {

  test("dts") {
    """
      |export function sumVarArgs(...is: number[]): number
      |""".check()
  }

}

object VarArgsTest {

  @JSExportTopLevel("sumVarArgs")
  def sum(is: Int*): Int = is.sum
}





