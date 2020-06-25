package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeArg

import scala.meta.Mod
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Output {

  // derive Interface instances for all opaque type
  def interfaces(opaqueTypes: List[isb.Type], symTab: SymbolTable): List[Interface] = {
    opaqueTypes.flatMap(_.typeSymbol).flatMap(symTab.info(_)).map(Interface(_, symTab))
  }

  def unions(exports: List[Input]): List[Union] = {

    val exportedClasses = exports.collect {
      case e: Input.Cls => e.si.symbol -> e
    }.toMap

    val exportedObjects = exports.collect {
      case e: Input.Obj => e.si.symbol -> e
    }.toMap

    val sealedTraitExports = exports.collect {
      case e: Input.Trait if e.tree.mods.exists(_.isInstanceOf[Mod.Sealed]) => e
    }

    val subtypes = SealedTraitSubtypeAnalyzer.subtypes(sealedTraitExports, exportedClasses, exportedObjects)

    sealedTraitExports.map(e => e -> subtypes.get(e.si.symbol)).collect {
      case (st, Some(l)) => Union(st, l.map(subtype => Output.UnionMember(subtype.unionMemberName, subtype.completeSubtypeArgs)))
    }

  }

  //
  //
  //

  sealed trait Type {
    def fullName: FullName
    def simpleName = fullName.last
  }

  //

  case class Interface private (fullName: FullName, typeParams: Seq[String], parents: Seq[ParentType], members: List[Input]) extends Type

  object Interface {

    def apply(si: SymbolInformation, symTab: SymbolTable): Interface = apply(si, Nil, symTab)

    def apply(si: SymbolInformation, member: List[Input], symTab: SymbolTable): Interface = {
      apply(si, FullName(si), member, symTab)
    }

    def apply(si: SymbolInformation, fullName: FullName, member: List[Input], symTab: SymbolTable): Interface = {
      new Interface(
        fullName,
        si.typeParamDisplayNames(symTab),
        ParentType.parentTypes(si, symTab),
        member
      )
    }

  }

  //

  case class Alias(e: Input.Alias) extends Type {
    def fullName: FullName = FullName(e.si)
    def rhs = e.typeSignature.lowerBound
  }

  //

  case class Union private (sealedTrait: Input.Trait, members: Seq[UnionMember]) extends Type {
    def fullName: FullName = FullName(sealedTrait.si).withUnionSuffix
    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = sealedTrait.classSignature.typeParamDisplayNames(symTab)
  }

  object Union {
    def apply(sealedTrait: Input.Trait, members: Seq[UnionMember]): Union = new Union(sealedTrait, members.sortBy(_.name.str))
  }

  case class UnionMember(name: FullName, typeArgs: Seq[SubtypeArg])

}
