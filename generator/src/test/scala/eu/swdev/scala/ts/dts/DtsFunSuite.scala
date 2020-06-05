package eu.swdev.scala.ts.dts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

trait DtsFunSuite extends AnyFunSuite with DtsGeneration with Matchers { self =>

  implicit def str2Ops(str: String) = new StrOps(str)

  case class StrOps(expectedDts: String) {

    /**
     * Generates the type declaration file for the given classes and checks if it matches the expected value.
     *
     * If no class is specified then the test class itself is used.
     */
    def check(classes: Class[_]*): Unit = {
      val generatedDts = if (classes.isEmpty) dts(self.getClass) else dts(classes: _*)
      generatedDts  mustBe expectedDts.stripMargin.trim
    }
  }

}
