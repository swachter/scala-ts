package eu.swdev.scala

import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.{SymbolInformation, Range => SRange}
import scala.meta.internal.symtab.SymbolTable

package object ts {

  implicit class PositionOps(val pos: Position) extends AnyVal {
    def includes(range: SRange): Boolean = {
      val s = pos.startLine < range.startLine || pos.startLine == range.startLine && pos.startColumn <= range.startCharacter
      val e = pos.endLine > range.endLine || pos.endLine == range.endLine && pos.endColumn >= range.endCharacter
      s && e
    }
  }

  implicit class SymbolTableOps(val symbolTable: SymbolTable) extends AnyVal {

    def typeParameter(symbol: String): Option[SymbolInformation] = symbolTable.info(symbol).filter(_.kind == Kind.TYPE_PARAMETER)
  }

}
