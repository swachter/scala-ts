package e2e

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

object SimpleAdt {

  sealed trait Adt

  @JSExportTopLevel("SimpleAdtCase1")
  @JSExportAll
  case class Case1(int: Int) extends Adt {
    val tpe: "i" = "i"
  }
  @JSExportTopLevel("SimpleAdtCase2")
  @JSExportAll
  case class Case2(str: String) extends Adt {
    val tpe: "s" = "s"
  }
}

object ObjectAdt {

  sealed trait Adt

  @JSExportTopLevel("ObjectAdtCase1")
  @JSExportAll
  object Case1 extends Adt {
    val tpe: 1 = 1
    val str    = "abc"
  }

  @JSExportTopLevel("ObjectAdtCase2")
  @JSExportAll
  object Case2 extends Adt {
    val tpe: 2 = 2
    val num    = 555
  }
}
