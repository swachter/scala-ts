package test

import eu.swdev.scala.ts.AdapterFunSuite
import eu.swdev.scala.ts.annotation.AdaptAll

class ObjectDefValVarTest extends AdapterFunSuite {
  """
    |import scala.scalajs.js
    |import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
    |import eu.swdev.scala.ts.adapter._
    |@JSExportTopLevel("Adapter")
    |object Adapter extends js.Object {
    |  object test extends js.Object {
    |    object ObjectDefValVarTest extends js.Object {
    |      def method(x: Int) = _root_.test.ObjectDefValVarTest.method(x).$res
    |      def value = _root_.test.ObjectDefValVarTest.value.$res
    |      def variable = _root_.test.ObjectDefValVarTest.variable.$cnv[Boolean]
    |      def variable_=(value: Boolean) = _root_.test.ObjectDefValVarTest.variable = value.$cnv[Boolean]
    |    }
    |  }
    |}
    |""".check()
}

@AdaptAll
object ObjectDefValVarTest {

  def method(x: Int): Boolean = ???
  val value: String = ???
  var variable: Boolean = ???

}