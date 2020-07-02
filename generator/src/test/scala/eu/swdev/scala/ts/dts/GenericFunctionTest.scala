package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class GenericFunctionTest extends DtsFunSuite {

  """
    |export function some<T>(t: T): scala.Some<T>
    |export function option<T>(t: T): scala.Option<T>
    |export const none: scala.None$
    |export namespace scala {
    |  interface None$ extends scala.Option<never> {
    |    'scala.None$': never
    |  }
    |  interface Option<A> {
    |    'scala.Option': never
    |  }
    |  interface Some<A> extends scala.Option<A> {
    |    'scala.Some': never
    |  }
    |}
    |""".check()

}

object GenericFunctionTest {

  @JSExportTopLevel("some")
  def some[T](t: T): Some[T] = Some(t)

  @JSExportTopLevel("option")
  def option[T](t: T): Option[T] = Some(t)

  @JSExportTopLevel("none")
  val none = None

}
