package eu.swdev.scala.ts

import eu.swdev.scala.ts.Input.Trait

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef}
import scala.meta.internal.{semanticdb => isb}

object SealedTraitSubtypeAnalyzer {

  /**
    * Calculates a map from symbols of sealed traits to their list of subtypes
    */
  def subtypes(
                sealedTraits: List[Trait],
                exportedClasses: Map[Symbol, Input.Cls],
                exportedObjects: Map[Symbol, Input.Obj],
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
          (exportedClasses.get(subtypeSymbolInformation.symbol), exportedObjects.get(subtypeSymbolInformation.symbol), result.get(subtypeSymbolInformation.symbol)) match {
            case (Some(e), _, _) => Some(Subtype.Subclass(parent, e))
            case (_, Some(e), _) => Some(Subtype.Subobject(parent, e))
            case (_, _, Some(l)) => Some(Subtype.Subtrait(parent, subtypeSymbolInformation, l))
            case (_, _, _) =>
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
    case class Subclass(parent: Input.Trait, subcls: Input.Cls) extends Subtype {
      override def unionMemberName     = subcls.name.map(_.toFullName).getOrElse(FullName(subcls.si.symbol))
      override def classSignature      = subcls.classSignature
      override def completeSubtypeArgs = localSubtypeArgs
    }
    case class Subobject(parent: Input.Trait, subobj: Input.Obj) extends Subtype {
      override def unionMemberName     = subobj.name.map(_.toFullName).getOrElse(FullName(subobj.si.symbol))
      override def classSignature      = subobj.si.signature.asInstanceOf[ClassSignature]
      override def completeSubtypeArgs = localSubtypeArgs
    }
    case class OpaqueSubclass(parent: Input.Trait, subclsSymbolInfo: SymbolInformation) extends Subtype {
      override def unionMemberName     = FullName(subclsSymbolInfo.symbol)
      override def classSignature      = subclsSymbolInfo.signature.asInstanceOf[ClassSignature]
      override def completeSubtypeArgs = localSubtypeArgs
    }
    case class Subtrait(parent: Input.Trait, subtraitSymbolInfo: SymbolInformation, subtypes: List[Subtype]) extends Subtype {
      override def unionMemberName = FullName(subtraitSymbolInfo.symbol)
      override def classSignature  = subtraitSymbolInfo.signature.asInstanceOf[ClassSignature]
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
