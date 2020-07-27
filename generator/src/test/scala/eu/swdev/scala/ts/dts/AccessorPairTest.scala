package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AccessorPairTest extends DtsFunSuite {

  """
    |export interface AccessorPairTest$ {
    |  prop: string
    |  'AccessorPairTest$': never
    |}
    |export const AccessorPairTest: AccessorPairTest$
    |""".check()

}


@JSExportTopLevel("AccessorPairTest")
@JSExportAll
object AccessorPairTest {
  def prop                  = Holder.x
  def prop_=(v: String)     = Holder.x = v

  private object Holder {
    var x = "abc"
  }

}
