package eu.swdev.scala

import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.{ClassSignature, SingleType, SymbolInformation, TypeRef, Range => SRange}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

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

    def isTypeParameter(symbol: String): Boolean = typeParameter(symbol).isDefined
  }

  implicit class ClassSignatureOps(val classSignature: ClassSignature) {
    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = classSignature.typeParameters match {
      case Some(s) => s.symlinks.map(symTab.info(_).get.displayName)
      case None    => Seq()
    }
  }

  implicit class TypeOps(val tpe: isb.Type) {

    def isTypeParameter(symTab: SymbolTable): Boolean = typeSymbol.map(symTab.isTypeParameter(_)).getOrElse(false)

    def typeSymbol: Option[String] = tpe match {
      case TypeRef(isb.Type.Empty, symbol, _) => Some(symbol)
      case SingleType(isb.Type.Empty, symbol) => Some(symbol)
      case _                                  => None
    }
  }

  type Symbol = String

  def fullName(symbol: Symbol): FullName = FullName(symbol.substring(0, symbol.length - 1).replace('/', '.'))

}
