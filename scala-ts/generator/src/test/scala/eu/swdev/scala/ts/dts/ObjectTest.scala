package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class ObjectTest extends DtsFunSuite {

  test("dts") {
    """
      |export const SomeObject: {
      |  doIt(): void
      |  readonly x:  number
      |  y:  number
      |}
      |""".check()
  }

}

@JSExportTopLevel("SomeObject")
@JSExportAll
object ObjectTest {

  def doIt() = ()

  val x = 1
  var y = 2

  private val a = 0
  private var b = 1
  private def m = ()

}