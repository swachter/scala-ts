package eu.swdev.scala.ts

import eu.swdev.scala.ts
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

trait AdapterFunSuite extends AnyFunSuite with ScalaMetaHelper with Matchers { self =>

  implicit def str2Ops(str: String) = new StrOps(str)

  case class StrOps(expectedAdapter: String) {

    /**
      * Generates the type declaration file for the given classes and checks if it matches the expected value.
      *
      * If no class is specified then the test class itself is used.
      */
    def check(classes: Class[_]*): Unit = {
      test("adapter") {
        val generatedAdapter = if (classes.isEmpty) adapter(self.getClass) else adapter(classes: _*)
        generatedAdapter mustBe expectedAdapter.stripMargin.trim
      }
    }
  }

  /**
    * Generate the type declaration file for all Scala Meta text documents that contains symbol information for any
    * of the given classes.
    *
    * Note that not only the given classes are considered when generating the type declaration file but all
    * symbols that are defined in the corresponding files.
    */
  def adapter(classes: Class[_]*): String = {

    val classSymbols = classes
      .map(_.getName.replace('.', '/'))
      .map { n =>
        if (n.endsWith("$")) {
          // symbol of a Scala object class
          s"${n.dropRight(1)}."
        } else {
          // symbol of a standard class
          s"$n#"
        }
      }
      .toSet

    val semSources = locateSemSources(metaInfPath, dialect).filter(_.td.symbols.map(_.symbol).exists(classSymbols.contains))

    val inputs = semSources.sortBy(_.td.uri).flatMap(Analyzer.analyze(_, symTab))

    AdapterGenerator.generate(inputs, symTab, "Adapter").trim
  }

}
