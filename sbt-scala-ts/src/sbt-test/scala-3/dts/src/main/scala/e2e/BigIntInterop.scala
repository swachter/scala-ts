package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("BigIntInterop")
object BigIntInterop extends js.Object {

  def string2BigInt(str: String): js.BigInt = js.BigInt(str)

  def bigInt2String(bigInt: js.BigInt): String = bigInt.toString()

}
