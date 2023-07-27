package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSName}

@JSExportTopLevel("BracketNotation")
object BracketNotation extends js.Object {

  @JSName("!a")
  val a = 1

  @JSName("!b")
  var b = 1

  @JSName("!c")
  def c() = 1

  @JSName("!d")
  def d(n: Int) = n

  @JSName("!e")
  object o extends js.Object {
    @JSName("!f")
    val x = 1
  }

}
