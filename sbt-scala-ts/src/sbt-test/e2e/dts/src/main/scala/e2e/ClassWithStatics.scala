package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportStatic, JSExportTopLevel}

/* .d.ts:

export class ClassWitStatics {
  static twice(x: number): number
  static readonly str: string
  static numero: number
  static get x(): number
  static set x(i: number)
  constructor()
}

 */

@JSExportTopLevel("ClassWitStatics")
class ClassWitStatics extends js.Object

object ClassWitStatics {

  @JSExportStatic
  def twice(x: Int) = 2 * x
  @JSExportStatic
  val str = "abc"
  @JSExportStatic("numero")
  var num = 55
  @JSExportStatic
  def x = _x
  @JSExportStatic
  def x_=(i: Int) = _x = i

  var _x = 0

}
