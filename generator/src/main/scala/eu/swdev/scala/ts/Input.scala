package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{ClassSignature, MethodSignature, SymbolInformation, TypeSignature, ValueSignature}
import scala.meta.{Defn, Term}

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
    val s = sym.substring(0, sym.length - 1).replace('/', '.').replace(".package.", ".")
    s"$s$suffix"
  }
  def apply(si: SymbolInformation): FullName       = fromSymbol(si.symbol)
  def fromSymbol(symbol: Symbol): FullName         = new FullName(symbol2TypeName(symbol))
  def fromSimpleName(simpleName: String): FullName = new FullName(simpleName)

  implicit val ord = implicitly[Ordering[String]].on[FullName](_.str)
}

sealed trait ExportAnnot {
  def isMember: Boolean
  def isStatic: Boolean
  def topLevelExportName: Option[String]
}

object ExportAnnot {
  sealed trait Member extends ExportAnnot {
    override def isMember: Boolean                  = true
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = None
  }
  sealed trait Static extends ExportAnnot {
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
  case class TopLevel(s: String) extends ExportAnnot {
    override def isMember: Boolean                  = false
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = Some(s)
  }
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
    def name: Option[ExportAnnot]
    def memberName: String = name match {
      case Some(ExportAnnot.MemberWithName(n)) => n
      case Some(ExportAnnot.StaticWithName(n)) => n
      case _                                   => si.displayName
    }
    def isTopLevelExport: Boolean = name match {
      case Some(ExportAnnot.TopLevel(_)) => true
      case _                             => false
    }
  }

  sealed trait Defn extends Input

  sealed trait Type extends Defn

  sealed trait DefOrValOrVar extends Defn with Exportable with HasMethodSignature

  sealed trait ClsOrObj extends Type with Exportable {
    def member: List[Defn]
  }

  //
  //
  //

  case class Def(semSrc: SemSource, tree: Defn.Def, name: Option[ExportAnnot], si: SymbolInformation) extends DefOrValOrVar
  case class Val(semSrc: SemSource, tree: Defn.Val, name: Option[ExportAnnot], si: SymbolInformation) extends DefOrValOrVar
  case class Var(semSrc: SemSource, tree: Defn.Var, name: Option[ExportAnnot], si: SymbolInformation) extends DefOrValOrVar

  case class Obj(semSrc: SemSource,
                 tree: Defn.Object,
                 name: Option[ExportAnnot],
                 si: SymbolInformation,
                 member: List[Defn],
                 expAll: Boolean)
      extends ClsOrObj {
    // indicates if this object is an exported member
    def isExportedMember = expAll || name.map(_.isMember).getOrElse(false)
  }

  case class Cls(semSrc: SemSource,
                 tree: Defn.Class,
                 name: Option[ExportAnnot],
                 si: SymbolInformation,
                 member: List[Defn],
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
