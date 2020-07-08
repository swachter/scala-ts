package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AbstractClassTest2 extends DtsFunSuite {

  """
    |export abstract class Base<X> {
    |  constructor(x: X)
    |  readonly x: X
    |  abstract size(): number
    |}
    |export class Case1 extends Base<string> {
    |  constructor(s: string)
    |  size(): number
    |}
    |export class Case2 extends Base<number> {
    |  constructor(i: number)
    |  size(): number
    |}
    |""".check()

}

object AbstractClassTest2 {

  @JSExportTopLevel("Base")
  abstract class Base[X](val x: X) extends js.Object {
    def size(): Int
  }

  @JSExportTopLevel("Case1")
  class Case1(s: String) extends Base(s) {
    override def size(): Int = s.length
  }
  @JSExportTopLevel("Case2")
  class Case2(i: Int) extends Base(i) {
    override def size(): Int = i
  }
}
