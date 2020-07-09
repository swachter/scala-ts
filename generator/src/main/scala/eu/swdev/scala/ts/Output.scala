package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.{Subtype, SubtypeParam}

import scala.meta.Mod
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Output {

  // derive Interface instances for all opaque type
  def interfaces(opaqueTypes: List[isb.Type], symTab: SymbolTable): List[Interface] = {
    opaqueTypes.flatMap(_.typeSymbol).flatMap(symTab.info(_)).map(Interface(_, symTab))
  }

  def unions(inputs: List[Input], symTab: SymbolTable): List[Union] = {

    val exportedClasses = inputs.collect {
      case e: Input.Cls => e.si.symbol -> e
    }.toMap

    val exportedObjects = inputs.collect {
      case e: Input.Obj => e.si.symbol -> e
    }.toMap

    val sealedTraitExports = inputs.collect {
      case e: Input.Trait if e.tree.mods.exists(_.isInstanceOf[Mod.Sealed]) => e
    }

    val subtypes = SealedTraitSubtypeAnalyzer.subtypes(sealedTraitExports, exportedClasses, exportedObjects, symTab)

    sealedTraitExports.map(sealedTrait => sealedTrait -> subtypes.get(sealedTrait.si.symbol)).collect {
      case (st, Some(subtypes)) => Union(st, subtypes)
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

  case class Interface private (fullName: FullName, typeParamSyms: Seq[Symbol], parents: Seq[ParentType], members: List[Input]) extends Type

  object Interface {

    def apply(si: SymbolInformation, symTab: SymbolTable): Interface = apply(si, Nil, symTab)

    def apply(si: SymbolInformation, member: List[Input], symTab: SymbolTable): Interface = {
      apply(si, FullName(si), member, symTab)
    }

    def apply(si: SymbolInformation, fullName: FullName, member: List[Input], symTab: SymbolTable): Interface = {
      new Interface(
        fullName,
        si.typeParamSymbols,
        ParentType.parentTypes(si, symTab),
        member
      )
    }

  }

  //

  case class Alias(e: Input.Alias) extends Type {
    def fullName: FullName = FullName(e.si)
    def rhs                = e.typeSignature.lowerBound
  }

  //

  class Union(val sealedTrait: Input.Trait, val members: Seq[UnionMember]) extends Type {
    def fullName: FullName = FullName(sealedTrait.si).withUnionSuffix
  }

  object Union {
    def apply(sealedTrait: Input.Trait, subtypes: Seq[Subtype]): Union = {
      val members = subtypes.zipWithIndex.map(UnionMember.tupled)
      new Union(sealedTrait, members)
    }
  }

  /**
    * @param subtype the subtype this union member is based on
    * @param idx the index of this union member in its union
    */
  case class UnionMember(subtype: Subtype, idx: Int) {
    def name = subtype.unionMemberName
    // use the idx of the union member to disambiguate type parameters names
    val typeParams = subtype.subtypeParams(s"${idx}_")
  }

}
