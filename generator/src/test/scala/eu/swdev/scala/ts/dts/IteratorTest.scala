package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class IteratorTest extends DtsFunSuite {

  test("dts") {
    """
      |export const iterator: Iterator<string>
      |export const iterable: Iterable<number>
      |""".check()
  }

}

object IteratorTest {
  @JSExportTopLevel("iterator")
  val x: js.Iterator[String] = ???
  @JSExportTopLevel("iterable")
  val y: js.Iterable[Int] = ???
}










