package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

/**
  * Base class for tests that scan semantic db files and check for comments that start with a ".d.ts:" or "group:" mark.
  *
  * The package where the test class is located is the package where semantic db files are looked for. Subpackages
  * are also considered. If a semantic db file has a comment starting with ".d.ts:" then that comment specifies the
  * expected generated declaration file.
  *
  * Multiple files can be grouped and processed together resulting in a single declaration file. All files belonging
  * to the same group contain a comment that starts with "group:" followed by the same identifier.
  */
abstract class AbstractAutoDetectingTest extends AnyFunSuite with ScalaMetaHelper with Matchers {

  def dtsInfos(): List[(SemSource, ValidationInfo)] = {
    val relativeTestDir = basePath.relativize(testDirPath).toString
    locateSemSources(metaInfPath, dialect)
      .filter(_.td.uri.contains(relativeTestDir))
      .map(s => s -> ValidationInfo.dts(s))
      .collect {
        case (semSource, Some(dtsInfo)) => semSource -> dtsInfo
      }
  }

  val infos = dtsInfos()

  private def check(inputs: List[Input.Defn], expectedDts: String): Unit = {
    val generatedDts = Generator.generate(inputs, false, symTab, getClass.getClassLoader).trim
    generatedDts mustBe expectedDts
  }

  infos.foreach {

    case (semSrc, ValidationInfo.Expected(expectedDts)) =>
      test(s"file: ${semSrc.td.uri}") {
        val inputs = Analyzer.analyze(semSrc, symTab)
        check(inputs, expectedDts)
      }

    case (_, ValidationInfo.ExpectedAndGroup(expectedDts, group)) =>
      test(s"group: $group") {
        val inputs = infos
          .collect {
            case (semSrc, ValidationInfo.Group(`group`))          => semSrc
            case (semSrc, ValidationInfo.ExpectedAndGroup(_, `group`)) => semSrc
          }
          .flatMap(Analyzer.analyze(_, symTab))
        check(inputs, expectedDts)
      }

    case _ =>
  }

}
