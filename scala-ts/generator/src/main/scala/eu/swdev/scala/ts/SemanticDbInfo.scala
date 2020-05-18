package eu.swdev.scala.ts

import java.io.File
import java.nio.file.Path

import scala.meta.internal.semanticdb.{Locator, TextDocuments}

object SemanticDbInfo {

  def collectTextDocuments(files: Seq[File]): Map[Path, TextDocuments] = {
    val paths = files.map(_.toPath.toAbsolutePath).toList
    val mapBuilder = Map.newBuilder[Path, TextDocuments]
    Locator(paths) { (path, textDocuments) =>
      mapBuilder.+=((path, textDocuments))
    }
    mapBuilder.result()
  }
}

class SemanticDbInfo(files: Seq[File]) {

  val textDocuments = SemanticDbInfo.collectTextDocuments(files)

}
