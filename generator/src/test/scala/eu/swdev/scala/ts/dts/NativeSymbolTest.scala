package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel, JSName}

class NativeSymbolTest extends DtsFunSuite {

  """
    |export interface NativeSymbol$ {
    |  readonly someSymbol: symbol
    |  [Symbol.iterator](): Iterator<number>
    |  [NativeSymbol.someSymbol](x: number): number
    |  'NativeSymbol$': never
    |}
    |export const NativeSymbol: NativeSymbol$
    |""".check()

}

@JSExportTopLevel("NativeSymbol")
object NativeSymbolTest extends js.Object {

  val someSymbol: js.Symbol = ???

  @JSName(js.Symbol.iterator)
  def iterator(): js.Iterator[Int] = ???

  @JSName(someSymbol)
  def someMethod(x: Int): Int = x
}










