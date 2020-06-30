package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

class CtorParamExportTest extends DtsFunSuite {

  test("dts") {
    """
      |export class CtorParamClass {
      |  constructor(x: number, z: number)
      |  readonly x: number
      |  y: number
      |}
      |""".check()
  }

}

object CtorParamExportTest {

  @JSExportTopLevel("CtorParamClass")
  class C(@JSExport val x: Int, @JSExport("y")var z: Int)

}

