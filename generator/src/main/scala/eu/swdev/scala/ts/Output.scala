package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.Subtype

import scala.collection.mutable
import scala.meta.Mod
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab.SymbolTable

object Output {

  /**
   * Derives union types corresponding to sealed traits in the input.
   *
   * For each sealed trait that is included in the namespace, i.e. that is referenced in the exported API, a corresponding
   * union type is created. If a union member has a type that needs to be declared (i.e. it does not correspond to a
   * native symbol symbol* (global, imported, or exported) and there is not yet an interface for that type) then the
   * a corresponding interface is also is returned.
   *
   * @return Returns the derived unions and missing interfaces for union members.
   */
  def unions(inputs: Inputs, namespace: Namespace, symTab: SymbolTable): (List[Union], List[Interface]) = {

    val sealedTraitExports = inputs.flattened.collect {
      case e: Input.Trait if e.tree.mods.exists(_.isInstanceOf[Mod.Sealed]) => e
    }

    val subtypes = SealedTraitSubtypeAnalyzer.subtypes(sealedTraitExports, inputs, symTab)

    val us = sealedTraitExports.map(sealedTrait => sealedTrait -> subtypes.get(sealedTrait.si.symbol)).collect {
      case (st, Some(subtypes)) if subtypes.nonEmpty => Union(st, subtypes)
    }

    // collect the symbols of all sealed traits for which a union is exported because the sealed trait is referenced
    // in the API or it is part of an exported union
    // -> the symbols must be collected 'top down', i.e. parent sealed traits are processed before their children
    //    ('exportedness' of parents propagates to children)

    val isExported = mutable.Set.empty[Symbol]

    us.sortBy(_.sealedTrait.si.ancestors(symTab).size).foreach { union =>
      val sealedTraitSymbol = union.sealedTrait.si.symbol
      if (namespace.contains(FullName.fromSymbol(sealedTraitSymbol)) || isExported.contains(sealedTraitSymbol)) {
        isExported += sealedTraitSymbol
        union.members.foreach {
          case UnionMember(s: Subtype.SealedTrait, _) => isExported += s.subtraitSymbolInfo.symbol
          case _                                      =>
        }
      }
    }

    // collect the symbols of all sealed traits for which a union is exported because all of their children are exported
    // -> the symbols must be collected bottom up

    us.sortBy(-_.sealedTrait.si.ancestors(symTab).size).foreach { union =>
      val sealedTraitSymbol = union.sealedTrait.si.symbol
      if (!isExported.contains(sealedTraitSymbol)) {
        if (union.members.map(_.subtype).forall {
              case _: Subtype.ExportedCls => true
              case _: Subtype.ExportedObj => true
              case s: Subtype.Opaque      => namespace.contains(FullName.fromSymbol(s.subclsSymbolInfo.symbol))
              case s: Subtype.SealedTrait => isExported.contains(s.subtraitSymbolInfo.symbol)
            }) {
          isExported += sealedTraitSymbol
        }
      }
    }

    val exportedUnions = us.filter(u => isExported.contains(u.sealedTrait.si.symbol))

    // add interfaces for all not yet exported union members

    val missingInterfaces = exportedUnions.flatMap(_.members.map(_.subtype)).collect {
      case Subtype.Opaque(_, si) if !namespace.contains(FullName(si)) => Interface(si, symTab)
    }

    (exportedUnions, missingInterfaces)

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
      val members = subtypes.zipWithIndex.map((UnionMember.apply _).tupled)
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
    def typeParams(symTab: SymbolTable) = subtype.subtypeParams(s"${idx}_", symTab)
  }

}
