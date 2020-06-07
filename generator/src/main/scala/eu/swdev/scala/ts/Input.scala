package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{ClassSignature, MethodSignature, SymbolInformation, TypeSignature, ValueSignature}
import scala.meta.{Defn, Term}

case class SimpleName(str: String) extends AnyVal {
  override def toString: String = str
  def toFullName: FullName = FullName.fromSimpleName(this)
}

object SimpleName {
  implicit val ord = implicitly[Ordering[String]].on[SimpleName](_.str)
}

case class FullName private (str: String) extends AnyVal {
  override def toString: String = str
  def last: SimpleName = SimpleName(str.split('.').last)
  def head: SimpleName = SimpleName(str.split('.').head)
  def tail: Option[FullName] = {
    val idx = str.indexOf('.')
    if (idx >= 0) Some(new FullName(str.substring(idx + 1))) else None
  }
  def withDollar = new FullName(s"$str$$")
}

object FullName {
  def symbol2TypeName(sym: Symbol): String = sym.substring(0, sym.length - 1).replace('/', '.').replace(".package.", ".")
  def apply(symbol: Symbol): FullName = new FullName(symbol2TypeName(symbol))
  def fromSimpleName(simpleName: SimpleName): FullName = new FullName(simpleName.str)
  implicit val ord = implicitly[Ordering[String]].on[FullName](_.str)
}

sealed trait Input {
  def semSrc: SemSource
  def si: SymbolInformation
}

object Input {

  sealed trait HasClassSignature extends Input {
    def classSignature = si.signature.asInstanceOf[ClassSignature]
  }

  sealed trait HasMethodSignature extends Input {
    def methodSignature = si.signature.asInstanceOf[MethodSignature]
  }

  sealed trait HasValueSignature extends Input {
    def valueSignature = si.signature.asInstanceOf[ValueSignature]
  }

  sealed trait HasTypeSignature extends Input {
    def typeSignature = si.signature.asInstanceOf[TypeSignature]
  }

  sealed trait TopLevel extends Input

  sealed trait Type extends TopLevel

  sealed trait MemberOrCtorParam

  sealed trait Member extends HasMethodSignature with MemberOrCtorParam

  case class Def(semSrc: SemSource, tree: Defn.Def, name: SimpleName, si: SymbolInformation) extends TopLevel with Member
  case class Val(semSrc: SemSource, tree: Defn.Val, name: SimpleName, si: SymbolInformation) extends TopLevel with Member
  case class Var(semSrc: SemSource, tree: Defn.Var, name: SimpleName, si: SymbolInformation) extends TopLevel with Member

  case class Obj(semSrc: SemSource, tree: Defn.Object, name: Option[SimpleName], si: SymbolInformation, member: List[Member]) extends Type
  case class Cls(semSrc: SemSource, tree: Defn.Class, name: Option[SimpleName], si: SymbolInformation, member: List[Member], ctorParams: List[Input.CtorParam]) extends HasClassSignature with Type
  case class Trait(semSrc: SemSource, tree: Defn.Trait, si: SymbolInformation, member: List[Member]) extends HasClassSignature with Type
  case class Alias(semSrc: SemSource, tree: Defn.Type, si: SymbolInformation) extends HasTypeSignature with Type

  case class CtorParam(semSrc: SemSource, tree: Term.Param, name: SimpleName, si: SymbolInformation, mod: CtorParamMod) extends HasValueSignature with MemberOrCtorParam

  sealed trait CtorParamMod

  object CtorParamMod {
    object Val extends CtorParamMod
    object Var extends CtorParamMod
    object Loc extends CtorParamMod
  }

}
