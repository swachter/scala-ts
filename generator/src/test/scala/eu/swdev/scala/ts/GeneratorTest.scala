package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class GeneratorTest extends AnyFunSuite with Matchers {

  test("string escape") {
    TypeFormatter.escapeTypeScriptString("abc") mustBe "abc"
    TypeFormatter.escapeTypeScriptString("'") mustBe "\\'"
    TypeFormatter.escapeTypeScriptString("\"") mustBe "\\\""
    TypeFormatter.escapeTypeScriptString("\n") mustBe "\\n"
    TypeFormatter.escapeTypeScriptString("\u00e4\u00f6\u00fc") mustBe "\\u00e4\\u00f6\\u00fc"
  }

}
