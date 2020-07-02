package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportStatic, JSExportTopLevel}

class StaticTest extends DtsFunSuite {

  """
    |export class ClassWitStatics {
    |  static twice(x: number): number
    |  static readonly str: string
    |  static numero: number
    |  static get x(): number
    |  static set x(i: number)
    |  constructor()
    |}
    |""".check()

}

object StaticTest {

  @JSExportTopLevel("ClassWitStatics")
  class ClassWitStatics extends js.Object

  object ClassWitStatics {

    @JSExportStatic
    def twice(x: Int) = 2 * x
    @JSExportStatic
    val str = "abc"
    @JSExportStatic("numero")
    var num = 55
    @JSExportStatic
    def x: Int = ???
    @JSExportStatic
    def x_=(i: Int): Unit = ???

  }

}

