package eu.swdev.scala.ts

import eu.swdev.scala.ts
import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.Subtype
import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.Subtype.{ExportedSubclass, OpaqueSubclass, Subtrait}

import scala.meta.internal.semanticdb.TypeRef
import scala.meta.internal.{semanticdb => isb}

object Union {

  case class UnionMember(name: FullName, typeArgs: Seq[String])

  def unions(exports: List[Export]): List[Union] = {

    val exportedClasses = exports.collect {
      case e: Export.Cls => e.si.symbol -> e
    }.toMap

    val stExports = exports.collect {
      case e: Export.Trt => e
    }

    val subtypes = SealedTraitSubtypeAnalyzer.subtypes(stExports, exportedClasses)

    stExports.map(e => e -> subtypes.get(e.si.symbol)).collect {
      case (st, Some(l)) => union(st, l)
    }

  }

  def union(st: Export.Trt, subtypes: List[Subtype]): Union = {
    val members = subtypes.map { subtype =>
      UnionMember(memberName(subtype), memberTypeArgs(st, subtype))
    }
    Union(st, members.sortBy(_.name.str))
  }

  def memberName(subtype: Subtype): FullName = subtype match {
    case ExportedSubclass(export) => FullName(export.name.str)
    case OpaqueSubclass(si)       => fullName(si.symbol)
    case Subtrait(subtrait, _)    => fullName(subtrait.si.symbol)
  }

  def memberTypeArgs(st: Export.Trt, subtype: Subtype): List[String] = {
    subtype match {
      case ExportedSubclass(export) =>
        val tpeParamSymLinksOfSubclass = export.classSignature.typeParameters.toList.flatMap(_.symlinks).toSet
        val parent = export.classSignature.parents.collect {
          case t@TypeRef(isb.Type.Empty, symbol, tArgs) if symbol == st.si.symbol => t
        }.head
        parent.typeArguments.zipWithIndex.map {
          case (TypeRef(isb.Type.Empty, s, _), idx) if tpeParamSymLinksOfSubclass.contains(s) =>
            // a type parameter is used as type argument for the super trait
            // -> use the display name of the corresponding type argument of the super trait
            val tArgName = st.classSignature.typeParameters.toList
              .flatMap(_.symlinks)
              .map(sl => st.semSrc.symbolInfo(sl))
              .map(_.displayName)
              .apply(idx)
            tArgName
          case (tpe, _) =>
            // another type is used as type argument for the super trait
            // -> format that type
            ""
//            formatType(tpe)
        }.toList

      case OpaqueSubclass(si)    => Nil
      case Subtrait(subtrait, _) => Nil
    }
  }

}

import eu.swdev.scala.ts.Union._

case class Union(sealedTrait: Export.Trt, members: Seq[UnionMember]) {
  def fullName: FullName = ts.fullName(sealedTrait.si.symbol)
}
