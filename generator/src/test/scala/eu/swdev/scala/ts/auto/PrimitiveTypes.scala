package eu.swdev.scala.ts.auto

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/* .d.ts:

export interface PrimitiveTypes$ {
  readonly string: string
  readonly int: number
  readonly double: number
  readonly byte: number
  readonly short: number
  readonly float: number
  readonly char: scala.Char
  readonly long: scala.Long
  readonly bigInt: bigint
  'PrimitiveTypes$': never
}
export const PrimitiveTypes: PrimitiveTypes$
export namespace scala {
  interface Char {
    'scala.Char': never
  }
  interface Long {
    'scala.Long': never
  }
}
 */

@JSExportTopLevel("PrimitiveTypes")
@JSExportAll
object PrimitiveTypes {

  val string: String = ???
  val int: Int = ???
  val double: Double = ???
  val byte: Byte = ???
  val short: Short = ???
  val float: Float = ???
  val char: Char = ???
  val long: Long = ???
  val bigInt: js.BigInt = ???

}
