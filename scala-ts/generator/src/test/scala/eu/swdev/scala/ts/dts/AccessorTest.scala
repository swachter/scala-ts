package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AccessorTest extends DtsFunSuite {

  test("dts") {
    """
      |export const AccessorTest: {
      |  get property(): number
      |  set property(v: number)
      |}
      |""".check()
  }

}

@JSExportTopLevel("AccessorTest")
@JSExportAll
object AccessorTest {

  private var x = 1

  def property = x
  def property_=(v: Int): Unit = x = v

}



