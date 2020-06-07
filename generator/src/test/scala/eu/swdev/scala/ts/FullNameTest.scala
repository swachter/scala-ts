package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class FullNameTest extends AnyFunSuite with Matchers {

  test("tail") {
    FullName("dts.AccessorTest#").tail.map(_.str) mustBe Some("AccessorTest")
  }

}
