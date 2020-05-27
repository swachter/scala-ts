package eu.swdev.scala.ts.dts


import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class ClassTest extends DtsFunSuite {

  test("dts") {
    """
      |export class SomeClass {
      |  constructor(s: string, i: number, b: boolean)
      |  readonly s: string
      |  i: number
      |  doIt(): void
      |}
      |""".check()
  }

}

@JSExportTopLevel("SomeClass")
@JSExportAll
case class SomeClass(s: String, var i: Int, private val b: Boolean) {

  def doIt() = ()

  private def privateDef() = ()
  private val privateVal = 1
  private var privateVar = 2

}