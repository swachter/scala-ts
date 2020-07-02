package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class VarArgsTest extends DtsFunSuite {

  """
    |export function sumVarArgs(...is: number[]): number
    |""".check()

}

object VarArgsTest {

  @JSExportTopLevel("sumVarArgs")
  def sum(is: Int*): Int = is.sum
}





