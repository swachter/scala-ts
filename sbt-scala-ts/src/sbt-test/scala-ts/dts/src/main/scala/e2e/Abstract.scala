package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/* .d.ts:


 */

object Abstract {

  // abstract classes can not be exported at the moment: https://github.com/scala-js/scala-js/issues/4117

  /*
  @JSExportTopLevel("AbstractBase")
  abstract class Base(val length: Int) extends js.Object {
    def area(): Double
    def color: String
    val dimensions: Int
    var name: String
  }

  @JSExportTopLevel("Square")
  class Square(length: Int) extends Base(length) {
    override def area(): Double = length * length
    override def color: String = "red"
    override val dimensions: Int = 2
    override var name: String = "square"
  }
  */

}
