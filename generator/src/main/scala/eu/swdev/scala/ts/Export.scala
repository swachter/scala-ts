package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{ClassSignature, MethodSignature, SymbolInformation, ValueSignature}
import scala.meta.{Defn, Term}

case class SimpleName(str: String) extends AnyVal {
  override def toString: String = str
}

object SimpleName {
  implicit val ord = implicitly[Ordering[String]].on[SimpleName](_.str)
}

case class FullName(str: String) extends AnyVal {
  override def toString: String = str
  def last: SimpleName = SimpleName(str.split('.').last)
  def head: SimpleName = SimpleName(str.split('.').head)
  def tail: Option[FullName] = {
    val idx = str.indexOf('.')
    if (idx >= 0) Some(FullName(str.substring(idx + 1))) else None
  }
}

object FullName {
  implicit val ord = implicitly[Ordering[String]].on[FullName](_.str)
}

sealed trait Export {
  def semSrc: SemSource
  def si: SymbolInformation
}

object Export {

  sealed trait HasClassSignature extends Export {
    def classSignature = si.signature.asInstanceOf[ClassSignature]
  }

  sealed trait HasMethodSignature extends Export {
    def methodSignature = si.signature.asInstanceOf[MethodSignature]
  }

  sealed trait HasValueSignature extends Export {
    def valueSignature = si.signature.asInstanceOf[ValueSignature]
  }

  sealed trait TopLevel extends Export

  sealed trait Member extends Export

  case class Def(semSrc: SemSource, tree: Defn.Def, name: SimpleName, si: SymbolInformation) extends HasMethodSignature with TopLevel with Member
  case class Val(semSrc: SemSource, tree: Defn.Val, name: SimpleName, si: SymbolInformation) extends HasMethodSignature with TopLevel with Member
  case class Var(semSrc: SemSource, tree: Defn.Var, name: SimpleName, si: SymbolInformation) extends HasMethodSignature with TopLevel with Member

  case class Obj(semSrc: SemSource, tree: Defn.Object, name: SimpleName, si: SymbolInformation, member: List[Member]) extends TopLevel
  case class Cls(semSrc: SemSource, tree: Defn.Class, name: SimpleName, si: SymbolInformation, member: List[Member], ctorParams: List[Export.CtorParam]) extends HasClassSignature with TopLevel
  case class Trt(semSrc: SemSource, tree: Defn.Trait, si: SymbolInformation, member: List[Member]) extends HasClassSignature with TopLevel

  case class CtorParam(semSrc: SemSource, tree: Term.Param, name: SimpleName, si: SymbolInformation, mod: CtorParamMod) extends HasValueSignature

  sealed trait CtorParamMod

  object CtorParamMod {
    object Val extends CtorParamMod
    object Var extends CtorParamMod
    object Loc extends CtorParamMod
  }

}
