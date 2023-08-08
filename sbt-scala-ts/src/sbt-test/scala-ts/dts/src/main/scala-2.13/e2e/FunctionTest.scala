package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object FunctionTest {
  @JSExportTopLevel("fun0")
  def fun0[R](f: js.Function0[R]): R = f()
  @JSExportTopLevel("fun1")
  def fun1[T1, R](a: js.Array[T1], f: js.Function1[T1, js.Array[R]]): js.Array[R] = a.flatMap(f)
  @JSExportTopLevel("fun2")
  def fun2[T1, T2, R](a1: js.Array[T1], a2: js.Array[T2], f: js.Function2[T1, T2, R]): js.Array[R] = a1.zip(a2).map(f.tupled)
}
