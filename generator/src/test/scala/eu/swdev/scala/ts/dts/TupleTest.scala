package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class TupleTest extends DtsFunSuite {

  test("simple") {
    """
      |export const tuple2: [string, number]
      |export const tuple3: [string, number, boolean]
      |""".check()
  }
}

object TupleTest {
  @JSExportTopLevel("tuple2")
  val tuple2: js.Tuple2[String, Int] = ???
  @JSExportTopLevel("tuple3")
  val tuple3: js.Tuple3[String, Int, Boolean] = ???
}


