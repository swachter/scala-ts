import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import eu.swdev.scala.ts.adapter._

package x {
  package y {
    class SomeClass(val array: Array[Int]) {
      def sum: Int = array.sum
    }
    object SomeClass {
      def upper(s: String) = s.toUpperCase
      val x = 5
      var y = "abc"
    }
  }
}

@JSExportTopLevel("adapter")
object Adapter extends js.Object {

  @JSExportAll
  trait InstanceAdapter[D] {
    val $delegate: D
  }

  object x extends js.Object {
    object y extends js.Object {
      @JSExportAll
      trait SomeClass extends InstanceAdapter[_root_.x.y.SomeClass] {
        def array = $res($delegate.array)
        def sum   = $res($delegate.sum)
      }
      object SomeClass extends js.Object {
        def newInstance(array: js.Array[Int]) = new _root_.x.y.SomeClass(array.$cnv[Array[Int]])
        def newAdapter(delegate: _root_.x.y.SomeClass) = new SomeClass {
          override val $delegate = delegate
        }
        // access object def
        def upper(s: String): String = $res(_root_.x.y.SomeClass.upper(s).$cnv[String])
        // access object val
        def x = $res(_root_.x.y.SomeClass.x)
        // access object var
        def y = $res(_root_.x.y.SomeClass.y)
        def y_=(v: String) = _root_.x.y.SomeClass.y = v.$cnv[String]
      }
    }
  }

}
