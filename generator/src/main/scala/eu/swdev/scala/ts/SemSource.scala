package eu.swdev.scala.ts

import java.io.File
import java.nio.file.Path

import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.{Dialect, Source}
import scala.meta.internal.semanticdb.{Locator, SymbolInformation, SymbolOccurrence, TextDocument}
import scala.meta.tokens.Tokens

case class SemSource(td: TextDocument, source: Source, dialect: Dialect) {

  def symbolInfo(symbol: String): SymbolInformation = td.symbols.find(_.symbol == symbol).get

  def symbolInfo(pos: Position, kind: Kind): Option[SymbolInformation] = symbolInfos(pos, kind).headOption

  def symbolInfos(pos: Position, kind: Kind): Seq[SymbolInformation] = {
    td.occurrences.collect {
      case so if so.role == Role.DEFINITION && so.range.isDefined && pos.includes(so.range.get) => so -> so.range.get
    }.map {
      case (so, range) => td.symbols.find(si => si.kind == kind && si.symbol == so.symbol).map((_, range))
    }.collect {
      case Some(s) => s
    }.sortBy(_._2.startCharacter).map(_._1)
  }

  def symbolOccurrences(pos: Position, role: Role): Seq[SymbolOccurrence] = {
    td.occurrences.filter(so => so.range.map(pos.includes(_)).getOrElse(false) && so.role == role)
  }

  def tokens: Tokens = source.tokens(dialect)

}

object SemSource {

  def apply(td: TextDocument, dialect: Dialect): SemSource = {
    val source = dialect(td.text).parse[Source].get
    SemSource(td, source, dialect)
  }

  /**
   * Locates SemSources in the given directory.
   *
   * The returned SemSources are sorted by the uri of their contained text document.
   */
  def locate(dir: File, dialect: Dialect): List[SemSource] = locate(dir.toPath, dialect)

  /**
   * Locates SemSources in the given directory.
   *
   * The returned SemSources are sorted by the uri of their contained text document.
   */
  def locate(dir: Path, dialect: Dialect): List[SemSource] = {
    val builder = List.newBuilder[SemSource]
    Locator(dir) { (_, tds) =>
      tds.documents.foreach { td =>
        val semSource = SemSource(td, dialect)
        builder += semSource
      }
    }
    builder.result().sortBy(_.td.uri)
  }

}
