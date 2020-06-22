package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class GeneratorTest extends AnyFunSuite with Matchers {

  test("string escape") {
    TypeFormatter.escapeString("abc") mustBe "abc"
    TypeFormatter.escapeString("'") mustBe "\\'"
    TypeFormatter.escapeString("\"") mustBe "\\\""
    TypeFormatter.escapeString("\n") mustBe "\\n"
    TypeFormatter.escapeString("\u00e4\u00f6\u00fc") mustBe "\\u00e4\\u00f6\\u00fc"
  }

}
