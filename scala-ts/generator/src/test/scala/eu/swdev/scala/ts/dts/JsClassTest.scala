package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class JsClassTest extends DtsFunSuite {

  test("dts") {
    """
      |export class JsClass {
      |  constructor(str: string, num: number)
      |  readonly str: string
      |  num: number
      |  setNum(n: number): void
      |}
      |""".check()
  }

}

object JsClassTest {

  @JSExportTopLevel("JsClass")
  class JsClass(val str: String, var num: Int) extends js.Object {
    def setNum(n: Int): Unit = num = n
  }
}





