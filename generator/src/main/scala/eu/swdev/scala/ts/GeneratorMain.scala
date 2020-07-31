package eu.swdev.scala.ts

import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.SymbolTable

class GeneratorMain {

  protected def dialect: Dialect = scala.util.Properties.versionNumberString match {
    case s if s.startsWith("2.12.") => dialects.Scala212
    case s if s.startsWith("2.13.") => dialects.Scala213
    case _                          => dialects.Scala
  }

  protected def validate(semSrcs: List[SemSource], symTab: SymbolTable, generate: Inputs => String): Unit = {
    val infos  = semSrcs.map(s => s -> ValidationInfo.dts(s)).collect { case (semSource, Some(dtsInfo)) => semSource -> dtsInfo }
    var failed = false
    def check(name: String, inputs: Inputs, expected: String): Unit = {
      val actual = generate(inputs).trim
      if (actual != expected) {
        System.err.println(s"$name: failed\n====== expected:\n$expected\n==== actual:\n$actual\n====")
        failed = true
      } else {
        System.out.println(s"$name: passed")
      }
    }
    infos.foreach {
      case (semSrc, ValidationInfo.Expected(expectedDts)) =>
        val inputs = Analyzer.analyze(List(semSrc), symTab)
        check(s"file: ${semSrc.td.uri}", inputs, expectedDts)

      case (_, ValidationInfo.ExpectedAndGroup(expectedDts, group)) =>
        val semSrcs = infos
          .collect {
            case (semSrc, ValidationInfo.Group(`group`))               => semSrc
            case (semSrc, ValidationInfo.ExpectedAndGroup(_, `group`)) => semSrc
          }
        val inputs = Analyzer.analyze(semSrcs, symTab)
        check(s"group: $group", inputs, expectedDts)

      case _ =>
    }

    if (failed) {
      System.exit(1)
    }

  }

}
