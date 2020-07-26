package eu.swdev.scala.ts.adapter

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class ConverterTest extends AnyFunSuite with Matchers {

  test("Array<->Array") {
    def method[X](a: Array[X]) = a
    // TODO find an implemation that does not need a type annotation at the $cnv method
    val r: js.Array[Int] = $res(method(js.Array(1, 2, 3).$cnv[Array[Int]]))
    r(0) mustBe 1
    r(1) mustBe 2
    r(2) mustBe 3
  }

  test("Array<->List") {
    def method[X](a: List[X]) = a
    // TODO find an implemation that does not need a type annotation at the $cnv method
    val r: js.Array[Int] = $res(method(js.Array(1, 2, 3).$cnv[List[Int]]))
    r(0) mustBe 1
    r(1) mustBe 2
    r(2) mustBe 3
  }

  test("UndefOr<->Option") {
    def method[X](a: Option[X]) = a
    val r1: js.UndefOr[Int]     = $res(method(Option(1).orUndefined.$cnv /*[Option[Int]]*/ ))
    r1.isDefined mustBe true
    r1.get mustBe 1
    val r2: js.UndefOr[Int] = $res(method(Option.empty[Int].orUndefined.$cnv /*[Option[Int]]*/ ))
    r2.isDefined mustBe false
  }

  test("Array[UndefOr]<->Array[Option]") {
    def method[X](a: Array[Option[X]]) = a
    val r: js.Array[js.UndefOr[Int]]   = $res(method(js.Array(Option(1).orUndefined).$cnv[Array[Option[Int]]]))
    r(0).get mustBe 1
  }

  test("generic param") {
    def method[X](a: X) = a
    // just check compilation, i.e. if generic parameters are handled by the identity converter
    def conv[X](x: X) = $res(method(x))
    conv(1) mustBe 1
    conv("abc") mustBe "abc"
  }

  test("Function0") {
    def method(f: Function0[Int]) = f
    val r                         = method($res(method(() => 0)))
    r.apply() mustBe 0
  }

  test("Function1") {
    def method(f: Function1[Int, Int]) = f
    val r                              = method($res(method((p0) => p0)))
    r.apply(1) mustBe 1
  }

  test("Function2") {
    def method(f: Function2[Int, Int, Int]) = f
    val r                                   = method($res(method((p0, p1) => p0 + p1)))
    r.apply(1, 2) mustBe 3
  }

  test("Function3") {
    def method(f: Function3[Int, Int, Int, Int]) = f
    val r                                        = method($res(method((p0, p1, p2) => p0 + p1 + p2)))
    r.apply(1, 2, 3) mustBe 6
  }

  test("Function4") {
    def method(f: Function4[Int, Int, Int, Int, Int]) = f
    val r                                             = method($res(method((p0, p1, p2, p3) => p0 + p1 + p2 + p3)))
    r.apply(1, 2, 3, 4) mustBe 10
  }

  test("Function[Array]") {
    def method(f: Function1[Int, Array[Int]]) = f
    val r                                     = method($res(method((p0) => Array.fill(p0)(0))))
    r.apply(1).length mustBe 1
  }

  test("Tuple") {
    type T = (Option[Int], List[String], Array[Double])
    def method[X](a: T) = a
    // TODO find an implemation that does not need a type annotation at the $cnv method
    val r               = $res(method(js.Tuple3(Option.empty[Int].orUndefined, js.Array("abc"), js.Array(2.0)).$cnv[T]))
    r._1.isDefined mustBe false
    r._2(0) mustBe "abc"
    r._3(0) mustBe 2.0
  }

}
