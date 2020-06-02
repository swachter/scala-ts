package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, TypeRef}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Namespace {

  case class Interface(name: SimpleName, fullName: FullName, typeParams: Seq[String], parents: Seq[Parent])
  case class Parent(name: FullName, typeArgs: Seq[String])

  def apply(opaqueTypes: List[isb.Type], symTab: SymbolTable): Namespace = {
    val ns = new Namespace(SimpleName(""))
    val allOpaqueTypeSymbols = opaqueTypes.collect {
      case TypeRef(isb.Type.Empty, symbol, _) => symbol
    }.toSet
    opaqueTypes.foreach {
      case TypeRef(isb.Type.Empty, symbol, _) =>
        symTab.info(symbol).foreach { si =>
          // it is a referenced class from the classpath

          // determine the enclosing namespace
          var n = ns
          symbol.split('/').dropRight(1).foreach { p =>
            n = ns.nested.getOrElseUpdate(p, new Namespace(SimpleName(p)))
          }

          val cs         = si.signature.asInstanceOf[ClassSignature]
          val typeParams = cs.typeParameters.fold(Seq.empty[String])(_.symlinks.map(typeParamDisplayName(_, symTab)))
          val parents = cs.parents.collect {
            case TypeRef(isb.Type.Empty, symbol, typeArguments) if allOpaqueTypeSymbols.contains(symbol) =>
              val typeArgs = typeArguments.map {
                case TypeRef(prefix, symbol, tas) =>
                  typeParamDisplayName(symbol, symTab)
              }
              Parent(fullName(symbol), typeArgs)

          }

          val itf = Interface(SimpleName(si.displayName), fullName(symbol), typeParams, parents)

          n.itfs(si.displayName) = itf

        }
      case _ =>
    }
    ns
  }

  def typeParamDisplayName(symLink: String, symTab: SymbolTable): String = {
    symTab.info(symLink).get.displayName
  }

  def fullName(symbol: String): FullName = FullName(symbol.substring(0, symbol.length - 1).replace('/', '.'))
}

class Namespace(val name: SimpleName) {
  import eu.swdev.scala.ts.Namespace.Interface

  val nested = mutable.SortedMap.empty[String, Namespace]
  val itfs   = mutable.SortedMap.empty[String, Interface]

}
