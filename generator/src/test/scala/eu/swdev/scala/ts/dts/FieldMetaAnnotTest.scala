package eu.swdev.scala.ts.dts

import scala.annotation.meta.field
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

class FieldMetaAnnotTest extends DtsFunSuite {

  test("dts") {
    """
      |export class FieldExportClass {
      |  constructor(x: number, z: number)
      |  readonly x: number
      |  y: number
      |}
      |""".check()
  }

}

object FieldMetaAnnotTest {

  @JSExportTopLevel("FieldExportClass")
  class C(@(JSExport @field)val x: Int, @(JSExport @field)("y")var z: Int)

}

