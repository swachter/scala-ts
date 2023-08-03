package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{
  ClassSignature,
  MethodSignature,
  Signature,
  SymbolInformation,
  TypeSignature,
  ValueSignature,
  Type => SdbType
}
import scala.meta.{Defn, Stat, Term}

case class FullName private (str: String) extends AnyVal {
  override def toString: String = str
  def last: String              = str.split('.').last
  def head: String              = str.split('.').head
  def tail: Option[FullName] = {
    val idx = str.indexOf('.')
    if (idx >= 0) Some(new FullName(str.substring(idx + 1))) else None
  }
  def withUnionSuffix      = new FullName(s"$str$$u")
  def member(name: String) = new FullName(s"$str.$name")
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
      .replace("_empty_/", "")
      .replace('/', '.') // path steps to packages
      .replace(".package.", ".")
      .replace('#', '.') // classes have a '#' suffix -> replace by '.' separator
    s"$s$suffix"
  }
  def apply(si: SymbolInformation): FullName       = fromSymbol(si.symbol)
  def fromSymbol(symbol: Symbol): FullName         = new FullName(symbol2TypeName(symbol))
  def fromSimpleName(simpleName: String): FullName = new FullName(simpleName)

  implicit val ord: Ordering[FullName] = implicitly[Ordering[String]].on[FullName](_.str)
}

case class Inputs(list: List[Input.Defn]) {

  lazy val flattened: List[Input.Defn] = {
    val b = List.newBuilder[Input.Defn]
    def go(i: Input.Defn): Unit = {
      b += i
      i match {
        case i: Input.ClsOrObj => i.member.foreach(go)
        case i: Input.Trait    => i.member.foreach(go)
        case _                 =>
      }
    }
    list.foreach(go)
    b.result()
  }

}

/**
  * Indicates the visibility of classes, objects, traits, defs, vals, and vars.
  *
  * The visibility is determined by various ScalaJS annotations or if a class, object, trait extends js.Any.
  */
sealed trait Visibility {
  def isMember: Boolean
  def isStatic: Boolean
  def isTopLevel: Boolean = topLevelExportName.isDefined
  def topLevelExportName: Option[String]
}

object Visibility {

  sealed trait Member extends Visibility {
    override def isMember: Boolean                  = true
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = None
  }

  sealed trait Static extends Visibility {
    override def isMember: Boolean                  = false
    override def isStatic: Boolean                  = true
    override def topLevelExportName: Option[String] = None
  }

  case object No extends Visibility {
    override def isMember: Boolean                  = false
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = None
  }

  /**
    * Used for members that have no annotation of their own but are visible because their owner is a subtype of
    * js.Any or has an @JSExportAll annotation.
    */
  case object DisplayName extends Member

  case object JSExportWithoutName        extends Member
  case class JSExportWithName(s: String) extends Member

  case class JSExportStaticWithName(s: String) extends Static
  case object JSExportStaticWithoutName        extends Static

  case class TopLevel(s: String) extends Visibility {
    override def isMember: Boolean                  = false
    override def isStatic: Boolean                  = false
    override def topLevelExportName: Option[String] = Some(s)
  }
  case class JsNameWithString(s: String) extends Member
  case class JsNameWithSymbol(s: Symbol) extends Member
}

/**
  * Indicates if a def, val, var, or parameter is adapted.
  */
sealed trait Adapted {
  def isAdapted: Boolean
  def interopType: Option[String]
}

object Adapted {
  case object No extends Adapted {
    override def isAdapted: Boolean = false
    def interopType: Option[String] = None
  }
  case object WithDefaultInteropType extends Adapted {
    override def isAdapted: Boolean = true
    def interopType: Option[String] = None
  }
  case class WithOverriddenInteropType(iopType: String) extends Adapted {
    override def isAdapted: Boolean = true
    def interopType: Option[String] = Some(iopType)
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

//  sealed trait HasMethodSignature { self: Input =>
//    def methodSignature = si.signature.asInstanceOf[MethodSignature]
//  }
//
//  sealed trait HasValueSignature { self: Input =>
//    def valueSignature = si.signature.asInstanceOf[ValueSignature]
//  }

  sealed trait HasTypeOrReturnType { self: Input =>
    def typeOrReturnType = si.signature match {
      case ValueSignature(tpe)               => tpe
      case MethodSignature(_, _, returnType) => returnType
      case s                                 => throw new Exception(s"Expected a ValueSignature or a MethodSignature but got a $s")
    }
  }

  sealed trait HasTypeSignature { self: Input =>
    def typeSignature = si.signature.asInstanceOf[TypeSignature]
  }

  sealed trait Exportable extends Input {
    def visibility: Visibility
    def isTopLevelExport: Boolean = visibility.isTopLevel
  }

  sealed trait Defn extends Input

  sealed trait Type extends Defn

  sealed trait DefOrValOrVar extends Defn with HasTypeOrReturnType with Exportable {
    def adapted: Adapted
    def typeParamSymbols: Seq[String] = si.signature match {
      case ValueSignature(tpe)                   => Seq.empty
      case MethodSignature(typeParameters, _, _) => typeParameters.typeParamSymbols
      case s                                     => throw new Exception(s"Expected a ValueSignature or a MethodSignature but got a $s")
    }
    def parameterLists = si.signature match {
      case ValueSignature(tpe)                   => Seq.empty
      case MethodSignature(_, parameterLists, _) => parameterLists
      case s                                     => throw new Exception(s"Expected a ValueSignature or a MethodSignature but got a $s")
    }
  }

  sealed trait ClsOrObj extends Type with Exportable {
    def allMembersAreVisible: Boolean
    def member: List[Defn]
  }

  //
  //
  //

  case class Def(semSrc: SemSource, tree: Stat, visibility: Visibility, adapted: Adapted, si: SymbolInformation, isAbstract: Boolean)
      extends DefOrValOrVar
  case class Val(semSrc: SemSource, tree: Stat, visibility: Visibility, adapted: Adapted, si: SymbolInformation, isAbstract: Boolean)
      extends DefOrValOrVar
  case class Var(semSrc: SemSource, tree: Stat, visibility: Visibility, adapted: Adapted, si: SymbolInformation, isAbstract: Boolean)
      extends DefOrValOrVar

  case class Obj(semSrc: SemSource,
                 tree: Defn.Object,
                 visibility: Visibility,
                 si: SymbolInformation,
                 member: List[Defn],
                 allMembersAreVisible: Boolean,
                 allMembersAreAdapted: Boolean)
      extends ClsOrObj {
    def isVisibleMember = visibility.isMember
  }

  case class Cls(semSrc: SemSource,
                 tree: Defn.Class,
                 visibility: Visibility,
                 constrAdapted: Boolean,
                 si: SymbolInformation,
                 member: List[Defn],
                 allMembersAreVisible: Boolean,
                 allMembersAreAdapted: Boolean,
                 ctorParams: List[Input.CtorParam],
                 isAbstract: Boolean)
      extends ClsOrObj
      with HasClassSignature

  case class Trait(semSrc: SemSource, tree: Defn.Trait, si: SymbolInformation, member: List[Defn]) extends Type with HasClassSignature

  case class Alias(semSrc: SemSource, tree: Defn.Type, si: SymbolInformation) extends Type with HasTypeSignature

  case class CtorParam(semSrc: SemSource,
                       tree: Term.Param,
                       name: String,
                       si: SymbolInformation,
                       mod: CtorParamMod,
                       isVisible: Boolean,
                       adapted: Adapted)
      extends Input
      with HasTypeOrReturnType

  sealed trait CtorParamMod

  object CtorParamMod {
    case class Val(name: String) extends CtorParamMod
    case class Var(name: String) extends CtorParamMod
    // private constructor param
    object Prv extends CtorParamMod
  }

}

case class TopLevelExport(name: String, exportable: Input.Exportable)
