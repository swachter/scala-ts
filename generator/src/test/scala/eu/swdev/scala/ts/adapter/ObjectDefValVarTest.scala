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
    |  @JSExportAll
    |  trait InstanceAdapter[D] {
    |    val $delegate: D
    |  }
    |  object test extends js.Object {
    |    object ObjectDefValVarTest extends js.Object {
    |      def method(x: scala.Int) = $res(test.ObjectDefValVarTest.method(x))
    |      def value = $res(test.ObjectDefValVarTest.value)
    |      def variable = $res($delegate.variable
    |      def variable_=(value: scala.Boolean) = test.ObjectDefValVarTest.variable = value.$cnv[scala.Boolean]
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