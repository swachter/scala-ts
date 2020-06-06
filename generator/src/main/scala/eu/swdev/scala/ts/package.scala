package eu.swdev.scala

import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.{ClassSignature, Scope, SingleType, SymbolInformation, TypeRef, TypeSignature, Range => SRange}
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

  implicit class ClassSignatureOps(val classSignature: ClassSignature) extends AnyVal {
    def typeParamSymbols: Seq[String] = ts.typeParamSymbols(classSignature.typeParameters)
    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = ts.typeParamDisplayNames(classSignature.typeParameters, symTab)
  }

  implicit class TypeOps(val tpe: isb.Type) extends AnyVal {

    def isTypeParameter(symTab: SymbolTable): Boolean = typeSymbol.map(symTab.isTypeParameter(_)).getOrElse(false)

    def typeSymbol: Option[String] = tpe match {
      case TypeRef(isb.Type.Empty, symbol, _) => Some(symbol)
      case SingleType(isb.Type.Empty, symbol) => Some(symbol)
      case _                                  => None
    }
  }

  implicit class SymbolInformationOps(val si: SymbolInformation) extends AnyVal {

    def parents: Seq[isb.Type] = if (si.signature.isInstanceOf[ClassSignature]) {
      si.signature.asInstanceOf[ClassSignature].parents
    } else {
      Seq()
    }

    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = {
      if (si.signature.isInstanceOf[ClassSignature]) {
        ts.typeParamDisplayNames(si.signature.asInstanceOf[isb.ClassSignature].typeParameters, symTab)
      } else if (si.signature.isInstanceOf[TypeSignature]) {
        ts.typeParamDisplayNames(si.signature.asInstanceOf[isb.TypeSignature].typeParameters, symTab)
      } else {
        Seq()
      }
    }
  }

  type Symbol = String

  def fullName(symbol: Symbol): FullName = FullName(symbol2Classname(symbol))

  def simpleName(symbol: Symbol): SimpleName = SimpleName(symbol2Classname(symbol).split('.').last)

  def symbol2Classname(symbol: Symbol): String = symbol.substring(0, symbol.length - 1).replace('/', '.').replace(".package.", ".")

  def typeParamSymbols(s: Option[Scope]): Seq[String] = s.toSeq.flatMap(_.symlinks)

  def typeParamDisplayNames(s: Option[Scope], symTab: SymbolTable): Seq[String] = typeParamSymbols(s).map(symTab.info(_).get.displayName)
}
