package eu.swdev.scala.ts.dts

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class GenericClassTest extends DtsFunSuite {

  test("dts") {
    """
      |export class Box<T> {
      |  constructor(value: T)
      |  readonly value: T
      |  map<X>(f: scala.Function1<T,X>): Box<X>
      |}
      |export function box<T>(value: T): Box<T>
      |export function unbox<T>(b: Box<T>): T
      |export namespace scala {
      |    interface Function1<T1,R> {
      |      'scala.Function1': never
      |    }
      |}
      |""".check()
  }

}

object GenericClassTest {

  @JSExportTopLevel("Box")
  @JSExportAll
  case class Box[T](value: T) {

    def map[X](f: T => X) = Box(f(value))
  }

  @JSExportTopLevel("box")
  def box[T](value: T) = Box(value)

  @JSExportTopLevel("unbox")
  def unbox[T](b: Box[T]): T = b.value
}


