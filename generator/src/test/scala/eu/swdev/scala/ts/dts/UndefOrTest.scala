package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class UndefOrTest extends DtsFunSuite {

  """
    |export function undefOrInt(u?: number): number | undefined
    |export function undefOrGeneric<X>(u?: X): X | undefined
    |export class UndefOrClass<X> {
    |  constructor(x?: X, y?: X)
    |  readonly x?: X
    |  y?: X
    |  method(p?: X): X | undefined
    |}
    |""".check()

}

object UndefOrTest {

  @JSExportTopLevel("undefOrInt")
  def undefOrInt(u: UndefOr[Int]) = u
  @JSExportTopLevel("undefOrGeneric")
  def undefOrGeneric[X](u: UndefOr[X]) = u

  @JSExportTopLevel("UndefOrClass")
  @JSExportAll
  case class UndefOrClass[X](x: UndefOr[X], var y: UndefOr[X]) {
    def method(p: UndefOr[X]) = p
  }

}


