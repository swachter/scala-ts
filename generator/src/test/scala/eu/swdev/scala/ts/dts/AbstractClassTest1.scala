package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class AbstractClassTest1 extends DtsFunSuite {

  """
    |export interface Case1 extends dts.AbstractClassTest1$.Base<string> {
    |  'Case1': never
    |}
    |export class Case1 {
    |  constructor(s: string)
    |}
    |export interface Case2 extends dts.AbstractClassTest1$.Base<number> {
    |  'Case2': never
    |}
    |export class Case2 {
    |  constructor(i: number)
    |}
    |export namespace dts {
    |  namespace AbstractClassTest1$ {
    |    interface Base<X> {
    |      readonly x: X
    |      'dts.AbstractClassTest1$.Base': never
    |    }
    |  }
    |}
    |""".check()

}

object AbstractClassTest1 {

  @JSExportAll
  abstract class Base[X](val x: X)

  @JSExportTopLevel("Case1")
  class Case1(s: String) extends Base(s)
  @JSExportTopLevel("Case2")
  class Case2(i: Int) extends Base(i)
}
