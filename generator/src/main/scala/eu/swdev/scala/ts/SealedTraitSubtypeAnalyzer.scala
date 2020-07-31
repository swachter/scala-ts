package eu.swdev.scala.ts

import eu.swdev.scala.ts.Input.Trait

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object SealedTraitSubtypeAnalyzer {

  /**
    * Calculates a map from symbols of sealed traits to their list of subtypes
    *
    * The subtype lists are sorted by their union member name.
    */
  def subtypes(
      sealedTraits: List[Trait],
      inputs: Inputs,
      symTab: SymbolTable,
  ): Map[Symbol, List[Subtype]] = {

    val topLevel = Analyzer.topLevel(inputs)

    val exportedClasses = topLevel.collect {
      case TopLevelExport(_, e: Input.Cls) => e.si.symbol -> e
    }.toMap

    val exportedObjects = topLevel.collect {
      case TopLevelExport(_, e: Input.Obj) => e.si.symbol -> e
    }.toMap

    val result = mutable.Map.empty[Symbol, List[Subtype]]

    // process sealed traits that have more ancestors first
    // -> when a parent sealed trait is processed then the result for a derived sealed trait can be accessed
    val sorted = sealedTraits.sortBy(-_.si.ancestors(symTab).size)

    sorted.foreach { parent =>
      // -> for each such trait search for symbol information that is a direct subtype of the trait
      //    (this can be done by collecting the symbol definitions from the text document the sealed trait belongs to)
      val subtypeSymbolInformations = parent.semSrc.td.symbols.collect {
        case si if isSubtype(parent, si) => si
      }

      // -> for each such symbol map it into a Subtype instance
      //    (this step can use subtype lists for sealed traits from the result that were already determined)
      val subtypeList = subtypeSymbolInformations.map { subtypeSymbolInformation =>
        (exportedClasses.get(subtypeSymbolInformation.symbol),
         exportedObjects.get(subtypeSymbolInformation.symbol),
         result.get(subtypeSymbolInformation.symbol)) match {
          case (Some(e), _, _) => Some(Subtype.ExportedCls(parent, e))
          case (_, Some(e), _) => Some(Subtype.ExportedObj(parent, e))
          case (_, _, Some(l)) => Some(Subtype.SealedTrait(parent, subtypeSymbolInformation, l))
          case (_, _, _) =>
            subtypeSymbolInformation.signature match {
              case ClassSignature(typeParameters, parents, self, declarations) =>
                Some(Subtype.Opaque(parent, subtypeSymbolInformation))
              case _ => None
            }
        }

      }

      // convert Seq[Option[Subtype]] into Option[List[Subtype]]
      val optSubtypeList = subtypeList.foldLeft(Option(List.empty[Subtype]))((accu, subtype) => accu.flatMap(a => subtype.map(_ :: a)))
      optSubtypeList.foreach(l => result += parent.si.symbol -> l.sortBy(_.unionMemberName.str))

    }

    result.toMap
  }

  private def isSubtype(superTrait: Trait, si: SymbolInformation): Boolean = si.signature match {
    case ClassSignature(_, parents, _, _) if parents.exists {
          case TypeRef(isb.Type.Empty, symbol, tArgs) if symbol == superTrait.si.symbol => true
          case _                                                                        => false
        } =>
      true
    case _ => false
  }

  sealed trait Subtype extends Product with Serializable {
    def parent: Input.Trait
    def parentSymbol: Symbol = parent.si.symbol
    // the name of this subtype when it is used as a union member
    def unionMemberName: FullName

    /**
      * Retrieves the type parameters of this subtype.
      *
      * Subtypes are members of union types. In order to disambiguate their type parameters names a prefix is provided.
      */
    def subtypeParams(prefix: String, symTab: SymbolTable): Seq[SubtypeParam]

    def classSignature: ClassSignature

    // maps type parameters of this subtype into type argument indexes for the parent type
    // -> a type parameter of the subtype may be passed to the parent type
    // -> in that case the map contains an entry mapping the symbol of the subtype type parameter
    //    into its argument index for the parent type
    def typeArgumentsForParent(symTab: SymbolTable): Map[Symbol, Int] = {
      classSignature.parents
        .collect {
          case TypeRef(isb.Type.Empty, symbol, tArgs) if symbol == parentSymbol =>
            tArgs.zipWithIndex.flatMap {
              case (tpe, idx) => tpe.typeSymbol(symTab).map(_ -> idx)
            }
        }
        .flatten
        .toMap
    }

  }

  object Subtype {

    sealed trait NonSealedTraitSubtype extends Subtype {
      override def subtypeParams(prefix: String, symTab: SymbolTable): Seq[SubtypeParam] = {
        classSignature.typeParamSymbols.map { typeParamSymbol =>
          typeArgumentsForParent(symTab).get(typeParamSymbol) match {
            case Some(idx) => SubtypeParam.Parent(idx)
            case None      => SubtypeParam.Unrelated(prefix, typeParamSymbol)
          }
        }
      }
    }

    case class ExportedCls(parent: Input.Trait, subcls: Input.Cls) extends NonSealedTraitSubtype {
      override def unionMemberName =
        subcls.visibility.topLevelExportName.map(FullName.fromSimpleName(_)).getOrElse(FullName(subcls.si))
      override def classSignature = subcls.classSignature
    }

    case class ExportedObj(parent: Input.Trait, subobj: Input.Obj) extends NonSealedTraitSubtype {
      override def unionMemberName =
        subobj.visibility.topLevelExportName.map(s => FullName.fromSimpleName(s"$s$$")).getOrElse(FullName(subobj.si))
      override def classSignature = subobj.si.signature.asInstanceOf[ClassSignature]
    }

    /**
      * Used for subtypes that are non-sealed traits or that are non-exported classes or objects.
      */
    case class Opaque(parent: Input.Trait, subclsSymbolInfo: SymbolInformation) extends NonSealedTraitSubtype {
      override def unionMemberName = FullName(subclsSymbolInfo)
      override def classSignature  = subclsSymbolInfo.signature.asInstanceOf[ClassSignature]
    }

    case class SealedTrait(parent: Input.Trait, subtraitSymbolInfo: SymbolInformation, subtypes: List[Subtype]) extends Subtype {
      override def unionMemberName = FullName(subtraitSymbolInfo).withUnionSuffix
      override def classSignature  = subtraitSymbolInfo.signature.asInstanceOf[ClassSignature]
      // subtype parameters of this sealed trait are determined based on the subtype parameters of all its subtypes
      override def subtypeParams(prefix: String, symTab: SymbolTable) = {
        // maps the indexes of the type parameters of this sealed trait into their symbol
        // -> used to translate subtype parameters of union members that reference a type parameter of this sealed trait
        val unionTypeParamSyms = classSignature.typeParameters.typeParamSymbols.zipWithIndex.map(_.swap).toMap

        val params = subtypes.zipWithIndex.flatMap {
          case (subtype, idx) =>
            // use the idx of the subtype to disambiguate type parameters names
            subtype.subtypeParams(s"$prefix${idx}_", symTab).map {
              case SubtypeParam.Parent(idx) if typeArgumentsForParent(symTab).contains(unionTypeParamSyms(idx)) =>
                // the subtype references a type param of this trait that is passed through to the parent of this trait
                SubtypeParam.Parent(typeArgumentsForParent(symTab)(unionTypeParamSyms(idx)))
              case SubtypeParam.Parent(idx) =>
                // the subtype references a type param of this trait that is not passed through to the parent of this trait
                // -> the type param is unrelated to the parent
                SubtypeParam.Unrelated(prefix, unionTypeParamSyms(idx))
              case stp => stp
            }
        }

        val (parentParams, unrelatedParams) = SubtypeParam.removeDuplicatesAndSplit(params)

        parentParams ++ unrelatedParams
      }
    }
  }

  /**
    * Represents a type parameter for a subtype of a sealed trait that is part of a union.
    *
    * Two kinds of type parameters are distinguished:
    *
    *  1. Type parameters that are passed through to the parent type (e.g. type X in Some[X] extends Option[X])
    *  1. Type parameters that are unrelated to the parent type
    */
  sealed trait SubtypeParam

  object SubtypeParam {

    /**
      * A type parameter that is passed through to the parent type
      *
      * @param idx index of the corresponding parent type parameter
      */
    case class Parent(idx: Int) extends SubtypeParam

    /**
      * A type parameter that is unrelated to the parent type
      *
      * @param prefix used to disambiguate type parameters of subtypes corresponding to different union members
      * @param sym symbol of the type parameter
      */
    case class Unrelated(prefix: String, sym: Symbol) extends SubtypeParam

    def removeDuplicatesAndSplit(allTypeArgs: Seq[SubtypeParam]): (Seq[Parent], Seq[Unrelated]) = {
      val parentArgsSet = allTypeArgs.collect {
        case p @ Parent(_) => p
      }.toSet

      val parentArgs = parentArgsSet.toList.sortBy(_.idx)

      val privateArgsSet = allTypeArgs.collect {
        case u: Unrelated => u
      }.toSet

      val privateArgs = privateArgsSet.toList.sortBy(_.prefix)

      (parentArgs, privateArgs)
    }
  }

}
