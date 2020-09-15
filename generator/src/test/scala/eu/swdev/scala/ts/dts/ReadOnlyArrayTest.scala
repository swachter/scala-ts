package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite
import eu.swdev.scala.ts.tpe.ReadOnlyArray

import scala.scalajs.js.annotation.JSExportTopLevel

class ReadOnlyArrayTest extends DtsFunSuite {

  """
    |export const readOnlyArray: ReadonlyArray<number>
    |""".check()

}

object ReadOnlyArrayTest {

  @JSExportTopLevel("readOnlyArray")
  val readOnlyArray: ReadOnlyArray[Int] = ???
}


