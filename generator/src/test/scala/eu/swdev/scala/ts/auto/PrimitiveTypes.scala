package eu.swdev.scala.ts.auto

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/* .d.ts:

export interface PrimitiveTypes$ {
  readonly string: string
  readonly int: number
  readonly double: number
  'PrimitiveTypes$': never
}
export const PrimitiveTypes: PrimitiveTypes$

 */

@JSExportTopLevel("PrimitiveTypes")
@JSExportAll
object PrimitiveTypes {

  val string: String = ???
  val int: Int = ???
  val double: Double = ???

}
