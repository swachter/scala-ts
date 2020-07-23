

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class RootPackageObjectTest extends DtsFunSuite {

  """
    |export interface rootPackageObject$ {
    |  readonly x: RootPackageObjectTest$.x$
    |  'rootPackageObject$': never
    |}
    |export const rootPackageObject: rootPackageObject$
    |export namespace RootPackageObjectTest$ {
    |  interface x$ {
    |    readonly x: number
    |    'RootPackageObjectTest$.x$': never
    |  }
    |}
    |""".check()

}

@JSExportTopLevel("rootPackageObject")
object RootPackageObjectTest extends js.Object {
  object x extends js.Object {
    val x = 1
  }
}
