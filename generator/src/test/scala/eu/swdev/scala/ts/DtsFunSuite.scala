package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

trait DtsFunSuite extends AnyFunSuite with ScalaMetaHelper with Matchers { self =>

  implicit def str2Ops(str: String) = new StrOps(str)

  case class StrOps(expectedDts: String) {

    /**
      * Generates the type declaration file for the given classes and checks if it matches the expected value.
      *
      * If no class is specified then the test class itself is used.
      */
    def check(addRootNamespace: Boolean = false): Unit = {
      test("dts") {
        val generatedDts = dts(addRootNamespace, Seq(self.getClass))
        generatedDts mustBe expectedDts.stripMargin.trim
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
  def dts(addRootNamespace: Boolean, classes: Seq[Class[_]]): String = {

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
      .map { n =>
        if (n.contains("/")) {
          n
        } else {
          s"_empty_/$n"
        }
      }
      .toSet

    val allSemSources = locateSemSources(metaInfPath, dialect)
    val semSources = allSemSources.filter(_.td.symbols.map(_.symbol).exists(classSymbols.contains))

    val inputs = semSources.sortBy(_.td.uri).flatMap(Analyzer.analyze(_, symTab))

    Generator.generate(inputs, addRootNamespace, symTab, getClass.getClassLoader).trim
  }

}
