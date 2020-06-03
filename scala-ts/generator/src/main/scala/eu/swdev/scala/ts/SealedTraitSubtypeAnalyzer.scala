package eu.swdev.scala.ts

import eu.swdev.scala.ts.Export.Trt

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef}
import scala.meta.internal.{semanticdb => isb}

object SealedTraitSubtypeAnalyzer {

  def subtypes(
                sts: List[Trt],
                exportedClasses: Map[Symbol, Export.Cls],
  ): Map[Symbol, List[Subtype]] = {

    val result = mutable.Map.empty[Symbol, List[Subtype]]

    def go(): Map[Symbol, List[Subtype]] = {
      // search for sealed traits that have not yet a result
      val next = sts.filter(st => !result.contains(st.si.symbol)).map { st =>
        // -> for each such trait search for symbol information that is a direct subtype of the trait
        //    (this can be done by collecting the symbol definitions from the text document the sealed trait belongs to)
        val subtypeSymbols = st.semSrc.td.symbols.collect {
          case si if isSubtype(st, si) => si
        }
        // -> for each such symbol map it into a Subtype instance
        //    (this step can use subtype lists for sealed traits from the result that were already determined)
        val subtypeList = subtypeSymbols.map { si =>
          (exportedClasses.get(si.symbol), result.get(si.symbol)) match {
            case (Some(e), _) => Some(Subtype.ExportedSubclass(e))
            case (_, Some(l)) => Some(Subtype.Subtrait(st, l))
            case (_, _) =>
              si.signature match {
                case ClassSignature(typeParameters, parents, self, declarations) =>
                  Some(Subtype.OpaqueSubclass(si))
                case _ => None
              }
          }
        }

        if (subtypeList.forall(_.isDefined)) {
          val l = subtypeList.collect {
            case Some(s) => s
          }.toList
          Some(st.si.symbol -> l)
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

  sealed trait Subtype extends Product with Serializable

  object Subtype {
    case class ExportedSubclass(export: Export.Cls)                            extends Subtype
    case class OpaqueSubclass(si: SymbolInformation)                           extends Subtype
    case class Subtrait(subtrait: Export.Trt, subtypes: List[Subtype]) extends Subtype
  }

}
