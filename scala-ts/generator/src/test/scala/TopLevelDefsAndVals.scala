import scala.scalajs.js.annotation.JSExportTopLevel

object TopLevelDefsAndVals {

  type JSE = JSExportTopLevel

  @JSExportTopLevel("some")
  def some(n: Int): Option[Int] = Some(n)

  @scala.scalajs.js.annotation.JSExportTopLevel("immutable")
  val immutable = "abc"

  @JSE("mutable")
  var mutable = true

}

