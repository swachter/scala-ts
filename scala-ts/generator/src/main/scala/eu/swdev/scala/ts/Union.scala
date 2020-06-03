package eu.swdev.scala.ts

import eu.swdev.scala.ts
import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.{Subtype, SubtypeArg}
import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.Subtype.{ExportedSubclass, OpaqueSubclass, Subtrait}

import scala.meta.Mod
import scala.meta.internal.semanticdb.TypeRef
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Union {

  case class UnionMember(name: FullName, typeArgs: Seq[SubtypeArg])

  def unions(exports: List[Export]): List[Union] = {

    val exportedClasses = exports.collect {
      case e: Export.Cls => e.si.symbol -> e
    }.toMap

    val sealedTraitExports = exports.collect {
      case e: Export.Trt if e.tree.mods.exists(_.isInstanceOf[Mod.Sealed]) => e
    }

    val subtypes = SealedTraitSubtypeAnalyzer.subtypes(sealedTraitExports, exportedClasses)

    sealedTraitExports.map(e => e -> subtypes.get(e.si.symbol)).collect {
      case (st, Some(l)) => Union(st, l.map(subtype => UnionMember(subtype.unionMemberName, subtype.completeSubtypeArgs)))
    }

  }

}

import eu.swdev.scala.ts.Union._

case class Union(sealedTrait: Export.Trt, members: Seq[UnionMember]) {
  def fullName: FullName = FullName(s"${ts.fullName(sealedTrait.si.symbol).str}$$")
  def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = sealedTrait.classSignature.typeParamDisplayNames(symTab)
}
