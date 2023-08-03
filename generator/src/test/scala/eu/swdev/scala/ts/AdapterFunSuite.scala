package eu.swdev.scala.ts

import eu.swdev.scala.ts
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

trait AdapterFunSuite extends AnyFunSuite with ScalaMetaHelper with Matchers { self =>

  implicit def strOps(str: String): StrOps = StrOps(str)

  case class StrOps(expectedAdapter: String) {

    /**
      * Creates a test that generates the adapter code for the given classes and checks if it matches the expected value.
      *
      * If no classes are given then the test class itself is used.
      *
      * Note that not only the given classes are considered when generating the adapter code but all
      * symbols that are defined in the corresponding files.
      */
    def check(classes: Class[_]*): Unit = {
      test("adapter") {
        val is               = inputs(classes: _*)
        val generatedAdapter = AdapterGenerator.generate(is, symTab, "Adapter").trim
        generatedAdapter mustBe expectedAdapter.stripMargin.trim
      }
    }
  }
}
