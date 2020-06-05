package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class GeneratorTest extends AnyFunSuite with Matchers {

  test("string escape") {
    Generator.escapeString("abc") mustBe "abc"
    Generator.escapeString("'") mustBe "\\'"
    Generator.escapeString("\"") mustBe "\\\""
    Generator.escapeString("\n") mustBe "\\n"
    Generator.escapeString("\u00e4\u00f6\u00fc") mustBe "\\u00e4\\u00f6\\u00fc"
  }

}
