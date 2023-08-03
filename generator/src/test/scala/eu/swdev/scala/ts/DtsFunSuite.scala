package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

trait DtsFunSuite extends AnyFunSuite with ScalaMetaHelper with Matchers { self =>

  implicit def strOps(str: String): StrOps = StrOps(str)

  case class StrOps(expectedDts: String) {

    /**
      * Creates a test that generates the type declaration file for this test class and checks if it matches the expected value.
      */
    def check(addRootNamespace: Boolean = false): Unit = {
      test("dts") {
        val is           = inputs()
        val generatedDts = Generator.generate(is, addRootNamespace, symTab, getClass.getClassLoader).trim
        generatedDts mustBe expectedDts.stripMargin.trim
      }
    }
  }
}
