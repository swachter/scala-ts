package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{ClassSignature, MethodSignature, SymbolInformation, TypeSignature, ValueSignature}
import scala.meta.{Defn, Stat, Term}

case class FullName private (str: String) extends AnyVal {
  override def toString: String = str
  def last: String              = str.split('.').last
  def head: String              = str.split('.').head
  def tail: Option[FullName] = {
    val idx = str.indexOf('.')
    if (idx >= 0) Some(new FullName(str.substring(idx + 1))) else None
  }
  def withUnionSuffix = new FullName(s"$str$$u")
}

object FullName {
  def symbol2TypeName(sym: Symbol): String = {
    val suffix = if (sym.endsWith(".")) {
      "$"
    } else {
      ""
    }
    val s = sym
      .substring(0, sym.length - 1)
      .replace(".package.", ".")
      .replace(".", "$.") // objects have a '.' suffix -> mark objects by a '$'
      .replace('/', '.') // path steps to packages
      .replace('#', '.') // classes have a '#' suffix -> replace by '.' separator
    s"$s$suffix"
  }
  def apply(si: SymbolInformation): FullName       = fromSymbol(si.symbol)
  def fromSymbol(symbol: Symbol): FullName         = new FullName(symbol2TypeName(symbol))
  def fromSimpleName(simpleName: String): FullName = new FullName(simpleName)

  implicit val ord = implicitly[Ordering[String]].on[FullName](_.str)
}

sealed trait NameAnnot {
  def isMember: Boolean
  def isStatic: Boolean
  def topLevelExportName: Option[String]
}

object NameAnnot {
  sealed trait Member extends NameAnnot {
    override def isMember: Boolean                  = true
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = None
  }
  sealed trait Static extends NameAnnot {
    override def isMember: Boolean                  = false
    override def isStatic: Boolean                  = true
    override def topLevelExportName: Option[String] = None
  }
  case object MemberWithoutName extends Member
  case class MemberWithName(s: String) extends Member {
    override def topLevelExportName: Option[String] = None
  }
  case class StaticWithName(s: String) extends Static
  case object StaticWithoutName        extends Static
  case class TopLevel(s: String) extends NameAnnot {
    override def isMember: Boolean                  = false
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = Some(s)
  }
  case class JsNameWithString(s: String) extends Member
  case class JsNameWithSymbol(s: Symbol) extends Member
}

sealed trait Input {
  def semSrc: SemSource
  def si: SymbolInformation
}

object Input {

  sealed trait HasClassSignature { self: Input =>
    def classSignature = si.signature.asInstanceOf[ClassSignature]
  }

  sealed trait HasMethodSignature { self: Input =>
    def methodSignature = si.signature.asInstanceOf[MethodSignature]
  }

  sealed trait HasValueSignature { self: Input =>
    def valueSignature = si.signature.asInstanceOf[ValueSignature]
  }

  sealed trait HasTypeSignature { self: Input =>
    def typeSignature = si.signature.asInstanceOf[TypeSignature]
  }

  sealed trait Exportable extends Input {
    def name: Option[NameAnnot]
    def isTopLevelExport: Boolean = name match {
      case Some(NameAnnot.TopLevel(_)) => true
      case _                           => false
    }
  }

  sealed trait Defn extends Input

  sealed trait Type extends Defn

  sealed trait DefOrValOrVar extends Defn with Exportable with HasMethodSignature

  sealed trait ClsOrObj extends Type with Exportable {
    def allMembersAreVisible: Boolean
    def member: List[Defn]
  }

  //
  //
  //

  case class Def(semSrc: SemSource, tree: Stat, name: Option[NameAnnot], si: SymbolInformation, isAbstract: Boolean) extends DefOrValOrVar
  case class Val(semSrc: SemSource, tree: Stat, name: Option[NameAnnot], si: SymbolInformation, isAbstract: Boolean) extends DefOrValOrVar
  case class Var(semSrc: SemSource, tree: Stat, name: Option[NameAnnot], si: SymbolInformation, isAbstract: Boolean) extends DefOrValOrVar

  case class Obj(semSrc: SemSource,
                 tree: Defn.Object,
                 name: Option[NameAnnot],
                 si: SymbolInformation,
                 member: List[Defn],
                 allMembersAreVisible: Boolean,
  ) extends ClsOrObj {
    // indicates if this object is an exported member
    def isExportedMember = allMembersAreVisible || name.map(_.isMember).getOrElse(false)
  }

  case class Cls(semSrc: SemSource,
                 tree: Defn.Class,
                 name: Option[NameAnnot],
                 si: SymbolInformation,
                 member: List[Defn],
                 allMembersAreVisible: Boolean,
                 ctorParams: List[Input.CtorParam],
                 isAbstract: Boolean)
      extends ClsOrObj
      with HasClassSignature

  case class Trait(semSrc: SemSource, tree: Defn.Trait, si: SymbolInformation, member: List[Defn]) extends Type with HasClassSignature

  case class Alias(semSrc: SemSource, tree: Defn.Type, si: SymbolInformation) extends Type with HasTypeSignature

  case class CtorParam(semSrc: SemSource, tree: Term.Param, name: String, si: SymbolInformation, mod: CtorParamMod)
      extends Input
      with HasValueSignature

  sealed trait CtorParamMod

  object CtorParamMod {
    case class Val(name: String) extends CtorParamMod
    case class Var(name: String) extends CtorParamMod
    // private constructor param
    object Prv extends CtorParamMod
  }

}

case class TopLevelExport(name: String, exportable: Input.Exportable)
