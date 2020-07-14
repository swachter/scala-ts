package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class PromiseLikeTest extends DtsFunSuite {

  """
    |export const promiseLike: PromiseLike<number>
    |""".check()

}

object PromiseLikeTest {

  @JSExportTopLevel("promiseLike")
  val pl: js.Thenable[Int] = ???
}

