package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class NoneTest extends DtsFunSuite {

  """
    |export const none: scala.None$
    |export namespace scala {
    |  interface None$ {
    |    'scala.None$': never
    |  }
    |}
    |""".check()

}

object NoneTest {

  @JSExportTopLevel("none")
  val none = None

}
