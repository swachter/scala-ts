package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class FunctionTest extends DtsFunSuite {

  test("simple") {
    """
      |export function fun0<R>(f: () => R): void
      |export function fun1<T1,R>(f: (p1: T1) => R): void
      |export function fun2<T1,T2,R>(f: (p1: T1, p2: T2) => R): void
      |export function fun3<T1,R>(f: (p1: () => R, p2: (p1: T1) => R) => R): void
      |""".check()
  }
}

object FunctionTest {
  @JSExportTopLevel("fun0")
  def fun0[R](f: js.Function0[R]) = ()
  @JSExportTopLevel("fun1")
  def fun1[T1, R](f: js.Function1[T1, R]) = ()
  @JSExportTopLevel("fun2")
  def fun2[T1, T2, R](f: js.Function2[T1, T2, R]) = ()
  @JSExportTopLevel("fun3")
  def fun3[T1, R](f: js.Function2[js.Function0[R], js.Function1[T1, R], R]) = ()
}