package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("OuterClass")
class Outer extends js.Object {
  object mid extends js.Object {
    object innerMost1 extends js.Object {
      var x = 1
    }
  }
}

@JSExportTopLevel("OuterObject")
object Outer extends js.Object {
  object middle extends js.Object {
    object innerMost extends js.Object {
      var x = 1
    }
  }
}


