import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class RootPackageClassTest extends DtsFunSuite {

  """
    |export class RootPackageClass {
    |  constructor()
    |  readonly i: number
    |}
    |""".check()

}

@JSExportTopLevel("RootPackageClass")
@JSExportAll
class RootPackageClass {
  val i = 1
}




