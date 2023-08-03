package eu.swdev.scala.ts

import java.io.File
import java.nio.file.Path
import scala.meta.common.Convert
import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.{Dialect, Source}
import scala.meta.internal.semanticdb.{Locator, SymbolInformation, SymbolOccurrence, TextDocument}
import scala.meta.parsers.Parse
import scala.meta.tokens.Tokens

case class SemSource(td: TextDocument, source: Source, dialect: Dialect) {

  def symbolInfo(symbol: String): SymbolInformation = td.symbols.find(_.symbol == symbol).get

  def symbolInfo(pos: Position, kind: Kind): Option[SymbolInformation] = symbolInfos(pos, kind).headOption

  /**
   * Returns SymbolInformation instances that lie in the given range and or of the given kind.
   *
   * The returned sequence is ordered by start positions.
   */
  def symbolInfos(pos: Position, kind: Kind): Seq[SymbolInformation] = {
    val symbolOccurrencesInRange = symbolOccurrences(pos, Role.DEFINITION)
    val matchingSymbolInformationsOfKind = symbolOccurrencesInRange
      .flatMap {
        case so => td.symbols.collect {
          case si if si.kind == kind && si.symbol == so.symbol => (si, so.range.get)
        }
      }
    val sortedByStartPosition = matchingSymbolInformationsOfKind
      .sortWith {
        case ((_, r1), (_, r2)) => r1.startLine < r2.startLine || r1.startLine == r2.startLine && r1.startCharacter < r2.startCharacter
      }
    sortedByStartPosition.map(_._1)
  }

  def symbolOccurrences(pos: Position, role: Role): Seq[SymbolOccurrence] = {
    td.occurrences.filter(so => so.range.map(pos.includes(_)).getOrElse(false) && so.role == role)
  }

  def tokens: Tokens = source.tokens(dialect)

}

object SemSource {

  def apply(td: TextDocument, dialect: Dialect): SemSource = {
    val parseSource   = implicitly[Parse[Source]]
    val stringToInput = implicitly[Convert[String, scala.meta.inputs.Input]]
    val input         = stringToInput.apply(td.text)
    val source        = parseSource.apply(input, dialect).get
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
