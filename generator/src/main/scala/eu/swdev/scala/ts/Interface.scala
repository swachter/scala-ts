package eu.swdev.scala.ts

import eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Interface {

  def apply(e: Export.Trt, symTab: SymbolTable): Interface = apply(e.si, e.member, symTab)

  def apply(si: SymbolInformation, symTab: SymbolTable): Interface = apply(si, Nil, symTab)

  def apply(si: SymbolInformation, member: List[Export.Member], symTab: SymbolTable): Interface = apply(si, simpleName(si.symbol), member, symTab)

  def apply(si: SymbolInformation, simpleName: SimpleName, member: List[Export.Member], symTab: SymbolTable): Interface = {
    Interface(
      si.symbol,
      simpleName,
      si.typeParamDisplayNames(symTab),
      ParentType.parentTypes(si),
      member
    )
  }

  // derive Interface instances for all opaque type
  def interfaces(opaqueTypes: List[isb.Type], symTab: SymbolTable): List[Interface] = {
    opaqueTypes.flatMap(_.typeSymbol).flatMap(symTab.info(_)).map(Interface(_, symTab))
  }

}

case class Interface(symbol: Symbol, simpleName: SimpleName, typeParams: Seq[String], parents: Seq[ParentType], members: List[Export.Member]) {
  val fullName = ts.fullName(symbol)
}

case class ParentType(fullName: FullName, typeArgs: Seq[isb.Type])

object ParentType {

  def parentTypes(si: SymbolInformation): Seq[ParentType] = {
    si.parents.collect {
      case TypeRef(isb.Type.Empty, symbol, typeArguments)  =>
        ParentType(fullName(symbol), typeArguments)
    }
  }

}

