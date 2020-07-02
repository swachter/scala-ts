package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AccessorTest extends DtsFunSuite {

  """
    |export interface AccessorTest$ {
    |  get property(): number
    |  set property(v: number)
    |  'AccessorTest$': never
    |}
    |export const AccessorTest: AccessorTest$
    |""".check()

}

@JSExportTopLevel("AccessorTest")
@JSExportAll
object AccessorTest {

  private var x = 1

  def property                 = x
  def property_=(v: Int): Unit = x = v

}
