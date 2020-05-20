package eu.swdev.scala.ts

import java.io.File

import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.{Dialect, Source, Tree}
import scala.meta.internal.semanticdb.{Locator, SymbolInformation, SymbolOccurrence, TextDocument}

case class SemSource(td: TextDocument, source: Source) {

  def symbolInfo(symbol: String): SymbolInformation = td.symbols.find(_.symbol == symbol).get

  def symbolInfo(pos: Position, kind: Kind): Option[SymbolInformation] = symbolInfos(pos, kind).headOption

  def symbolInfos(pos: Position, kind: Kind): Seq[SymbolInformation] = {
    td.occurrences
      .filter(so => so.range.map(pos.includes(_)).getOrElse(false))
      .map { so =>
        td.symbols.find(si => si.kind == kind && si.symbol == so.symbol)
      }
      .collect {
        case Some(s) => s
      }
  }

  def symbolOccurrences(pos: Position, role: Role): Seq[SymbolOccurrence] = {
    td.occurrences.filter(so => so.range.map(pos.includes(_)).getOrElse(false) && so.role == role)
  }

}

object SemSource {

  def apply(td: TextDocument, dialect: Dialect): SemSource = {
    val source = dialect(td.text).parse[Source].get
    SemSource(td, source)
  }

  def from(file: File, dialect: Dialect): List[SemSource] = {
    val path = file.toPath
    val builder = List.newBuilder[SemSource]
    Locator(path) { (_, tds) =>
      tds.documents.foreach { td =>
        val semSource = SemSource(td, dialect)
        builder += semSource
      }
    }
    builder.result()
  }

}
