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
    |      def traverse[X](ol: _root_.scala.scalajs.js.Iterable[_root_.scala.scalajs.js.UndefOr[X]]) = _root_.adapter.ListTest.traverse(ol.$cnv[_root_.scala.List[_root_.scala.Option[X]]]).$cnv[_root_.scala.scalajs.js.UndefOr[_root_.scala.scalajs.js.Iterable[X]]]
    |    }
    |  }
    |}
    |""".check()

}

@AdaptAll
object ListTest {

  def traverse[X](ol: List[Option[X]]): Option[List[X]] = ???
}