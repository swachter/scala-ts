package adapter

import eu.swdev.scala.ts.AdapterFunSuite
import eu.swdev.scala.ts.annotation.AdaptAll

class ArrayTest extends AdapterFunSuite {

  """
    |import scala.scalajs.js
    |import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
    |import eu.swdev.scala.ts.adapter._
    |@JSExportTopLevel("Adapter")
    |object Adapter extends js.Object {
    |  object adapter extends js.Object {
    |    object ArrayTest extends js.Object {
    |      def x = _root_.adapter.ArrayTest.x.$cnv[_root_.eu.swdev.scala.ts.tpe.ReadOnlyArray[Int]]
    |      def x_=(value: _root_.eu.swdev.scala.ts.tpe.ReadOnlyArray[Int]) = _root_.adapter.ArrayTest.x = value.$cnv[_root_.scala.Array[Int]]
    |    }
    |  }
    |}
    |""".check()

}

@AdaptAll
object ArrayTest {

  var x: Array[Int] = ???
}

