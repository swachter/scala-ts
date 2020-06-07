package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{SymbolInformation, TypeRef}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Interface {

  def apply(si: SymbolInformation, symTab: SymbolTable): Interface = apply(si, Nil, symTab)

  def apply(si: SymbolInformation, member: List[Input.MemberOrCtorParam], symTab: SymbolTable): Interface = {
    apply(si, FullName(si.symbol), member, symTab)
  }

  def apply(si: SymbolInformation, fullName: FullName, member: List[Input.MemberOrCtorParam], symTab: SymbolTable): Interface = {
    new Interface(
      fullName,
      si.typeParamDisplayNames(symTab),
      ParentType.parentTypes(si, symTab),
      member
    )
  }

  // derive Interface instances for all opaque type
  def interfaces(opaqueTypes: List[isb.Type], symTab: SymbolTable): List[Interface] = {
    opaqueTypes.flatMap(_.typeSymbol).flatMap(symTab.info(_)).map(Interface(_, symTab))
  }

}

case class Interface private (fullName: FullName, typeParams: Seq[String], parents: Seq[ParentType], members: List[Input.MemberOrCtorParam]) {
  def simpleName = fullName.last
}

case class ParentType(fullName: FullName, typeArgs: Seq[isb.Type])

object ParentType {

  def parentTypes(si: SymbolInformation, symTab: SymbolTable): Seq[ParentType] = {
    si.parents(symTab).collect {
      case TypeRef(isb.Type.Empty, symbol, typeArguments)  =>
        ParentType(FullName(symbol), typeArguments)
    }
  }

}

