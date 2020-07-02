package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class TopLevelDefValVarTest extends DtsFunSuite {

  """
    |export const topLevelDefValVar_valString: string
    |export const topLevelDefValVar_valBoolean: boolean
    |export const topLevelDefValVar_valInt: number
    |export const topLevelDefValVar_valDouble: number
    |export let topLevelDefValVar_varString: string
    |export let topLevelDefValVar_varBoolean: boolean
    |export let topLevelDefValVar_varInt: number
    |export let topLevelDefValVar_varDouble: number
    |export function topLevelDefValVar_defUnit(): void
    |export function topLevelDefValVar_defNothing(): never
    |export function topLevelDefValVar_defDouble(n: number): number
    |""".check()

}

object TopLevelDefValVarTest {

  @JSExportTopLevel("topLevelDefValVar_valString")
  val valString = "abc";
  @JSExportTopLevel("topLevelDefValVar_valBoolean")
  val valBoolean = true
  @JSExportTopLevel("topLevelDefValVar_valInt")
  val valInt = 1
  @JSExportTopLevel("topLevelDefValVar_valDouble")
  val valDouble = 1.0

  @JSExportTopLevel("topLevelDefValVar_varString")
  var varString = "abc";
  @JSExportTopLevel("topLevelDefValVar_varBoolean")
  var varBoolean = true
  @JSExportTopLevel("topLevelDefValVar_varInt")
  var varInt = 1
  @JSExportTopLevel("topLevelDefValVar_varDouble")
  var varDouble = 1.0

  @JSExportTopLevel("topLevelDefValVar_defUnit")
  def unit() = ()
  @JSExportTopLevel("topLevelDefValVar_defNothing")
  def nothing(): Nothing = throw new RuntimeException()
  @JSExportTopLevel("topLevelDefValVar_defDouble")
  def double(n: Double) = 2 * n

}
