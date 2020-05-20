import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

object TopLevelDefsAndVals {

  type JSE = JSExportTopLevel

  @JSExportTopLevel("some")
  def some(n: Int): Option[Int] = Some(n)

  @scala.scalajs.js.annotation.JSExportTopLevel("immutable")
  val immutable = "abc"

  @JSE("mutable")
  var mutable = true

  @JSExportTopLevel("obj")
  @JSExportAll
  object obj {
    def twice(n: Int) = 2 * n
    val immu = 7
    var mu = 6
  }

  @JSExportTopLevel("Cls")
  @JSExportAll
  case class Cls(i: Int, b: Boolean, s: String) {
    def doIt() = ()
  }


}

