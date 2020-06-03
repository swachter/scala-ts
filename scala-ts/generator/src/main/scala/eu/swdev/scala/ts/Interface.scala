package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Interface {

  def apply(e: Export.Trt, symTab: SymbolTable): Interface = apply(e.si, e.member, symTab)

  def apply(si: SymbolInformation, symTab: SymbolTable): Interface = apply(si, Nil, symTab)

  def apply(si: SymbolInformation, member: List[Export.Member], symTab: SymbolTable): Interface = {
    val cs = si.signature.asInstanceOf[ClassSignature]
    Interface(
      fullName(si.symbol),
      cs.typeParamDisplayNames(symTab),
      cs.parents.collect {
        case TypeRef(isb.Type.Empty, symbol, typeArguments)  =>
          Parent(fullName(symbol), typeArguments)
      },
      member
    )
  }

  case class Parent(fullName: FullName, typeArgs: Seq[isb.Type])

  // derive Interface instances for all opaque type
  def interfaces(opaqueTypes: List[isb.Type], symTab: SymbolTable): List[Interface] = {
    opaqueTypes.flatMap(_.typeSymbol).flatMap(symTab.info(_)).map(Interface(_, symTab))
  }

}

case class Interface(fullName: FullName, typeParams: Seq[String], parents: Seq[Interface.Parent], members: List[Export.Member]) {
  def simpleName: SimpleName = fullName.last
}

