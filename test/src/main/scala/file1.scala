package eu.swdev.scala.ts.sample.p1

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("Simple")
case class Simple(int: Int, string: String, boolean: Boolean, double: Double)

object Test {

  @JSExportTopLevel("simple")
  def simple(v: Simple): Simple = v

  @JSExportTopLevel("option")
  def option[T](t: T): Option[T] = Some(t)
}

object TopLevelDefValVar {

  @JSExportTopLevel("immutable")
  val immutable = 5
  @JSExportTopLevel("mutable")
  var mutable = "abc"
  @JSExportTopLevel("setMutable")
  def setMutable(s: String): Unit = mutable = s

  @JSExportTopLevel("multiply")
  def multiply(x: Int, y: Int) = x * y
}