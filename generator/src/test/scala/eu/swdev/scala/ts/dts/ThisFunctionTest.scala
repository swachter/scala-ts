package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.{ThisFunction1, ThisFunction2}
import scala.scalajs.js.annotation.JSExportTopLevel

class ThisFunctionTest extends DtsFunSuite {

  """
    |export const thisFunction1: (this: void, p1: string) => number
    |export const thisFunction2: (this: void, p1: string, p2: boolean) => number
    |export function thisMethod(this: void, i: number): number
    |""".check()

}

object ThisFunctionTest {
  @JSExportTopLevel("thisFunction1")
  val f1: ThisFunction1[Unit, String, Int] = ???
  @JSExportTopLevel("thisFunction2")
  val f2: ThisFunction2[Unit, String, Boolean, Int] = ???
  @JSExportTopLevel("thisMethod")
  def f3(`this`: Unit, i: Int): Int = i
}







