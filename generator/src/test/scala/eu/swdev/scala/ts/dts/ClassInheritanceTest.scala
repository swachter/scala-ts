package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class ClassInheritanceTest extends DtsFunSuite {

  test("dts") {
    """
      |export class A {
      |  constructor(n: number)
      |  readonly n: number
      |}
      |export class B extends A {
      |  constructor(n: number, s: string)
      |  readonly s: string
      |}
      |""".check()
  }

}

object ClassInheritanceTest {

  @JSExportTopLevel("A")
  @JSExportAll
  class A(val n: Int)

  @JSExportTopLevel("B")
  @JSExportAll
  class B(n: Int, val s: String) extends A(n)

}




