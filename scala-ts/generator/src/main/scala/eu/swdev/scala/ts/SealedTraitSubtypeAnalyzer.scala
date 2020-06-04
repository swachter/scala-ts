package eu.swdev.scala.ts

import eu.swdev.scala.ts.Export.Trt

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef}
import scala.meta.internal.{semanticdb => isb}

object SealedTraitSubtypeAnalyzer {

  /**
    * Calculates a map from symbols of sealed traits to their list of subtypes
    */
  def subtypes(
      sealedTraits: List[Trt],
      exportedClasses: Map[Symbol, Export.Cls],
  ): Map[Symbol, List[Subtype]] = {

    val result = mutable.Map.empty[Symbol, List[Subtype]]

    def go(): Map[Symbol, List[Subtype]] = {
      // search for sealed traits that have not yet a result
      val next = sealedTraits.filter(st => !result.contains(st.si.symbol)).map { parent =>
        // -> for each such trait search for symbol information that is a direct subtype of the trait
        //    (this can be done by collecting the symbol definitions from the text document the sealed trait belongs to)
        val subtypeSymbolInformations = parent.semSrc.td.symbols.collect {
          case si if isSubtype(parent, si) => si
        }
        // -> for each such symbol map it into a Subtype instance
        //    (this step can use subtype lists for sealed traits from the result that were already determined)
        val subtypeList = subtypeSymbolInformations.map { subtypeSymbolInformation =>
          (exportedClasses.get(subtypeSymbolInformation.symbol), result.get(subtypeSymbolInformation.symbol)) match {
            case (Some(e), _) => Some(Subtype.ExportedSubclass(parent, e))
            case (_, Some(l)) => Some(Subtype.Subtrait(parent, subtypeSymbolInformation, l))
            case (_, _) =>
              subtypeSymbolInformation.signature match {
                case ClassSignature(typeParameters, parents, self, declarations) =>
                  Some(Subtype.OpaqueSubclass(parent, subtypeSymbolInformation))
                case _ => None
              }
          }
        }

        if (subtypeList.forall(_.isDefined)) {
          val l = subtypeList.collect {
            case Some(s) => s
          }.toList
          Some(parent.si.symbol -> l)
        } else {
          None
        }
      }

      val n = next.collect {
        case Some(s) => s
      }

      if (n.isEmpty) {
        // no new mapping: symbol -> List[Subtype]
        // -> terminate
        result.toMap
      } else {
        // there are new mappings: symbol -> List[Subtype]
        // -> add the mappings to the result and recurse
        n.foreach(result += _)
        go()
      }
    }
    go()
  }

  private def isSubtype(superTrait: Trt, si: SymbolInformation): Boolean = si.signature match {
    case ClassSignature(_, parents, _, _) if parents.exists {
          case TypeRef(isb.Type.Empty, symbol, tArgs) if symbol == superTrait.si.symbol => true
          case _                                                                        => false
        } =>
      true
    case _ => false
  }

  sealed trait Subtype extends Product with Serializable {
    def parent: Export.Trt
    def parentSymbol: Symbol = parent.si.symbol
    def classSignature: ClassSignature
    def unionMemberName: FullName

    def localSubtypeArgs: Seq[SubtypeArg] = {

      // determine which symbols are used at which position when extending the parent type
      val typeArgumentsForParent = classSignature.parents
        .collect {
          case TypeRef(isb.Type.Empty, symbol, tArgs) if symbol == parentSymbol =>
            tArgs.zipWithIndex.flatMap {
              case (tpe, idx) => tpe.typeSymbol.map(_ -> idx)

            }
        }
        .flatten
        .toMap

      classSignature.typeParamSymbols.map { typeParamSymbol =>
        typeArgumentsForParent.get(typeParamSymbol) match {
          case Some(idx) => SubtypeArg.Parent(idx)
          case None      => SubtypeArg.Private
        }
      }

    }

    def completeSubtypeArgs: Seq[SubtypeArg]
  }

  object Subtype {
    case class ExportedSubclass(parent: Export.Trt, exportedSubclass: Export.Cls) extends Subtype {
      override def unionMemberName     = FullName(exportedSubclass.name.str)
      override def classSignature      = exportedSubclass.classSignature
      override def completeSubtypeArgs = localSubtypeArgs
    }
    case class OpaqueSubclass(parent: Export.Trt, subtypeSymbolInformation: SymbolInformation) extends Subtype {
      override def unionMemberName     = fullName(subtypeSymbolInformation.symbol)
      override def classSignature      = subtypeSymbolInformation.signature.asInstanceOf[ClassSignature]
      override def completeSubtypeArgs = localSubtypeArgs
    }
    case class Subtrait(parent: Export.Trt, subtypeSymbolInformation: SymbolInformation, subtypes: List[Subtype]) extends Subtype {
      override def unionMemberName = fullName(subtypeSymbolInformation.symbol)
      override def classSignature  = subtypeSymbolInformation.signature.asInstanceOf[ClassSignature]
      override def completeSubtypeArgs = {
        val allTypeArgs = subtypes.flatMap(_.completeSubtypeArgs)
        val (parentArgs, privateArgs) = SubtypeArg.split(allTypeArgs)
        parentArgs ++ privateArgs
      }
    }
  }

  sealed trait SubtypeArg

  object SubtypeArg {
    case class Parent(idx: Int) extends SubtypeArg
    case object Private         extends SubtypeArg

    def split(allTypeArgs: Seq[SubtypeArg]): (Seq[Parent], Seq[Private.type]) = {
      val parentArgsSet = allTypeArgs
        .collect {
          case p @ Parent(_) => p
        }
        .toSet

      val parentArgs = parentArgsSet.toList.sortBy(_.idx)

      val privateArgs = allTypeArgs.collect {
        case Private => Private
      }
      (parentArgs, privateArgs)
    }
  }

}
