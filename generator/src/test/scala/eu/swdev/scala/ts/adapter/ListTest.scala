package adapter

import eu.swdev.scala.ts.AdapterFunSuite
import eu.swdev.scala.ts.annotation.AdaptAll

class ListTest extends AdapterFunSuite {

  """
    |import scala.scalajs.js
    |import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
    |import eu.swdev.scala.ts.adapter._
    |@JSExportTopLevel("Adapter")
    |object Adapter extends js.Object {
    |  object adapter extends js.Object {
    |    object ListTest extends js.Object {
    |      def traverse[X](ol: js.Array[js.UndefOr[X]]) = _root_.adapter.ListTest.traverse(ol.$cnv[scala.List[scala.Option[X]]]).$res
    |    }
    |  }
    |}
    |""".check()

}

@AdaptAll
object ListTest {

  def traverse[X](ol: List[Option[X]]): Option[List[X]] = ???
}