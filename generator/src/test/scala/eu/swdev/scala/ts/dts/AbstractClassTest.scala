package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AbstractClassTest extends DtsFunSuite {

  """
    |export interface Case1 extends dts.AbstractClassTest.Base<string> {
    |  'Case1': never
    |}
    |export class Case1 {
    |  constructor(s: string)
    |}
    |export interface Case2 extends dts.AbstractClassTest.Base<number> {
    |  'Case2': never
    |}
    |export class Case2 {
    |  constructor(i: number)
    |}
    |export namespace dts {
    |  namespace AbstractClassTest {
    |    interface Base<X> {
    |      readonly x: X
    |      'dts.AbstractClassTest.Base': never
    |    }
    |  }
    |}
    |""".check()

}

object AbstractClassTest {

  @JSExportAll
  abstract class Base[X](val x: X)

  @JSExportTopLevel("Case1")
  class Case1(s: String) extends Base(s)
  @JSExportTopLevel("Case2")
  class Case2(i: Int) extends Base(i)
}
