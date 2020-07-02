package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class BoundsTest extends DtsFunSuite {

  """
    |export function upperBound<T extends object>(t: T): void
    |export class UpperBound<T extends object> {
    |  constructor()
    |}
    |""".check()

}

object BoundsTest {

  @JSExportTopLevel("upperBound")
  def upperBound[T <: js.Object](t: T): Unit = ???

  @JSExportTopLevel("UpperBound")
  class UpperBound[T <: js.Object]
}
