package eu.swdev.scala.ts.converter

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class ConverterTest extends AnyFunSuite with Matchers {

  test("Array conversion") {
    def method[X](a: Array[X]) = a
    val r: js.Array[Int] = result(method(js.Array(1, 2, 3).convert[Array[Int]]))
    r(0) mustBe 1
    r(1) mustBe 2
    r(2) mustBe 3
  }

  test("UndefOr conversion") {
    def method[X](a: Option[X]) = a
    val r1: js.UndefOr[Int]     = result(method(Option(1).orUndefined.convert[Option[Int]]))
    r1.isDefined mustBe true
    r1.get mustBe 1
    val r2: js.UndefOr[Int] = result(method(Option.empty[Int].orUndefined.convert[Option[Int]]))
    r2.isDefined mustBe false
  }

  test("Array[UndefOr] conversion") {
    def method[X](a: Array[Option[X]]) = a
    val r: js.Array[js.UndefOr[Int]]   = result(method(js.Array(Option(1).orUndefined).convert[Array[Option[Int]]]))
    r(0).get mustBe 1
  }

  test("List conversion") {
    def method[X](a: List[X]) = a
    val r: js.Array[Int]      = result(method(js.Array(1, 2, 3).convert[List[Int]]))
    r(0) mustBe 1
    r(1) mustBe 2
    r(2) mustBe 3
  }

  test("generic param conversion") {
    def method[X](a: X) = a
    // just check compilation, i.e. if generic parameters are handled by the identity converter
    def conv[X](x: X) = result(method(x).convert[X])
    conv(1) mustBe 1
    conv("abc") mustBe "abc"
  }
}
