package eu.swdev.scala.ts

import eu.swdev.scala.ts
import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeArg

import scala.meta.Mod
import scala.meta.internal.symtab.SymbolTable

object Union {

  case class UnionMember(name: FullName, typeArgs: Seq[SubtypeArg])

  def unions(exports: List[Export]): List[Union] = {

    val exportedClasses = exports.collect {
      case e: Export.Cls => e.si.symbol -> e
    }.toMap

    val exportedObjects = exports.collect {
      case e: Export.Obj => e.si.symbol -> e
    }.toMap

    val sealedTraitExports = exports.collect {
      case e: Export.Trt if e.tree.mods.exists(_.isInstanceOf[Mod.Sealed]) => e
    }

    val subtypes = SealedTraitSubtypeAnalyzer.subtypes(sealedTraitExports, exportedClasses, exportedObjects)

    sealedTraitExports.map(e => e -> subtypes.get(e.si.symbol)).collect {
      case (st, Some(l)) => Union(st, l.map(subtype => UnionMember(subtype.unionMemberName, subtype.completeSubtypeArgs)))
    }

  }

  def apply(sealedTrait: Export.Trt, members: Seq[UnionMember]): Union = new Union(sealedTrait, members.sortBy(_.name.str))

}

import eu.swdev.scala.ts.Union._

case class Union private (sealedTrait: Export.Trt, members: Seq[UnionMember]) {
  def fullName: FullName = FullName(s"${ts.fullName(sealedTrait.si.symbol).str}$$")
  def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = sealedTrait.classSignature.typeParamDisplayNames(symTab)
}
