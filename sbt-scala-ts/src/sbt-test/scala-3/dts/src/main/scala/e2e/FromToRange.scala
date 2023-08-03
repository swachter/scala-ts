package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSName}
import scala.scalajs.js.JSConverters._

@JSExportTopLevel("FromToRange")
class FromToRange(from: Int, to: Int) extends js.Object {

  @JSName(js.Symbol.iterator)
  def iterator(): js.Iterator[Int] = (from to to).iterator.toJSIterator
}
