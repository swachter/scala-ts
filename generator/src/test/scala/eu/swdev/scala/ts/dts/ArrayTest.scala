package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class ArrayTest extends DtsFunSuite {

  """
    |export function vector(v: (number)[]): (number)[]
    |export function vectorGen<X>(v: (X)[]): (X)[]
    |export function matrix(v: ((number)[])[]): ((number)[])[]
    |export function matrixGen<X>(v: ((X)[])[]): ((X)[])[]
    |""".check()

}

object ArrayTest {

  @JSExportTopLevel("vector")
  def vector(v: js.Array[Int]) = v
  @JSExportTopLevel("vectorGen")
  def vectorGen[X](v: js.Array[X]) = v
  @JSExportTopLevel("matrix")
  def matrix(v: js.Array[js.Array[Int]]) = v
  @JSExportTopLevel("matrixGen")
  def matrixGen[X](v: js.Array[js.Array[X]]) = v

}
