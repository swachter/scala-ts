package eu.swdev.scala.ts

import scala.meta.{Defn, Term}
import scala.meta.internal.semanticdb.{MethodSignature, SymbolInformation, ValueSignature}

case class SimpleName(str: String) extends AnyVal {
  override def toString: String = str
}

case class FullName(str: String) extends AnyVal {
  override def toString: String = str
}

sealed trait Export {
  def si: SymbolInformation
}

object Export {

  sealed trait HasMethodSignature extends Export {
    def methodSignature = si.signature.asInstanceOf[MethodSignature]
  }

  sealed trait HasValueSignature extends Export {
    def valueSignature = si.signature.asInstanceOf[ValueSignature]
  }

  sealed trait TopLevel extends Export

  sealed trait Member extends Export

  sealed trait CtorParam extends HasValueSignature {
    def name: SimpleName
  }

  case class Def(semSrc: SemSource, tree: Defn.Def, name: SimpleName, si: SymbolInformation) extends HasMethodSignature with TopLevel with Member
  case class Val(semSrc: SemSource, tree: Defn.Val, name: SimpleName, si: SymbolInformation) extends HasMethodSignature with TopLevel with Member
  case class Var(semSrc: SemSource, tree: Defn.Var, name: SimpleName, si: SymbolInformation) extends HasMethodSignature with TopLevel with Member

  case class Obj(semSrc: SemSource, tree: Defn.Object, name: SimpleName, si: SymbolInformation, member: List[Member]) extends TopLevel
  case class Cls(semSrc: SemSource, tree: Defn.Class, name: SimpleName, si: SymbolInformation, member: List[Member], ctorParams: List[Export.CtorParam]) extends TopLevel

  case class CVal(semSrc: SemSource, tree: Term.Param, name: SimpleName, si: SymbolInformation) extends CtorParam
  case class CVar(semSrc: SemSource, tree: Term.Param, name: SimpleName, si: SymbolInformation) extends CtorParam

}
