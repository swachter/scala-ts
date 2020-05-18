package eu.swdev.scala.ts

import java.io.File

import scala.meta.internal.semanticdb.TextDocument
import scala.meta.{Dialect, Source, dialects}

class Generator(
    di: DependencyInfo,
    si: SourceInfo,
    dialect: Dialect
) {

  def processTypes(textDocument: TextDocument, source: Source): List[String] = {
    Nil
  }

  def processFunctions(textDocument: TextDocument, source: Source): List[String] = {
    Nil
  }

  def processAll(): Unit = {

    val input = si.textDocuments.values
      .flatMap(_.documents)
      .map { textDocument =>
        val parsed = dialect(textDocument.text).parse[Source].get
        (textDocument, parsed)
      }
      .toList

    input.foreach((processFunctions _).tupled)

  }

  def generate(): String = {
    ""
  }

}

object Generator {

  def apply(
      sourceInfo: File,
      dependencyInfo: Set[File],
  ): String = {

    val di = DependencyInfo(dependencyInfo)
    val si = new SourceInfo(Seq(sourceInfo))

    new Generator(di, si, dialects.Scala212).generate()
  }
}
