package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class PromiseTest extends DtsFunSuite {

  """
    |export const promise: Promise<string>
    |""".check()

}

object PromiseTest {

  @JSExportTopLevel("promise")
  val promise: js.Promise[String] = ???
}







