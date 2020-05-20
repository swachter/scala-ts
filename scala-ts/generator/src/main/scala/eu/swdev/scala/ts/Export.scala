package eu.swdev.scala.ts

import scala.meta.Defn
import scala.meta.internal.semanticdb.{MethodSignature, Signature, SymbolInformation}

case class SimpleName(str: String) extends AnyVal {
  override def toString: String = str
}

sealed trait Export {
  def si: SymbolInformation
}

object Export {

  sealed trait DefValVar extends Export {
    def methodSignature = si.signature.asInstanceOf[MethodSignature]
  }

  case class Def(semSrc: SemSource, tree: Defn.Def, name: SimpleName, si: SymbolInformation) extends DefValVar
  case class Val(semSrc: SemSource, tree: Defn.Val, name: SimpleName, si: SymbolInformation) extends DefValVar
  case class Var(semSrc: SemSource, tree: Defn.Var, name: SimpleName, si: SymbolInformation) extends DefValVar
  case class Obj(semSrc: SemSource, tree: Defn.Object, name: SimpleName, si: SymbolInformation, member: List[DefValVar]) extends Export
  case class Cls(semSrc: SemSource, tree: Defn.Class, name: SimpleName, si: SymbolInformation, member: List[DefValVar]) extends Export

}
