package eu.swdev.scala

import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.{
  AnnotatedType,
  ByNameType,
  ClassSignature,
  ConstantType,
  ExistentialType,
  IntersectionType,
  MethodSignature,
  RepeatedType,
  Scope,
  SingleType,
  StructuralType,
  SuperType,
  SymbolInformation,
  ThisType,
  TypeRef,
  TypeSignature,
  UnionType,
  UniversalType,
  WithType,
  Range => SRange
}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

package object ts {

  case class ParentType(fullName: FullName, typeArgs: Seq[isb.Type])

  object ParentType {

    def parentTypes(si: SymbolInformation, symTab: SymbolTable): Seq[ParentType] = {
      si.parents(symTab).collect {
        case TypeRef(isb.Type.Empty, symbol, typeArguments)  =>
          ParentType(FullName.fromSymbol(symbol), typeArguments)
      }
    }

  }



  implicit class PositionOps(val pos: Position) extends AnyVal {
    def includes(range: SRange): Boolean = {
      val s = pos.startLine < range.startLine || pos.startLine == range.startLine && pos.startColumn <= range.startCharacter
      val e = pos.endLine > range.endLine || pos.endLine == range.endLine && pos.endColumn >= range.endCharacter
      s && e
    }
  }

  implicit class SymbolTableOps(val symbolTable: SymbolTable) extends AnyVal {

    def typeParamSymInfo(symbol: Symbol): Option[SymbolInformation] = symbolTable.info(symbol).filter(_.kind == Kind.TYPE_PARAMETER)

    def classSymInfo(symbol: Symbol): Option[SymbolInformation] = symbolTable.info(symbol).filter(_.kind == Kind.CLASS)

    /**
      * A type is a trait, a type alias, a class, or an object
      */
    def typeSymInfo(symbol: String): Option[SymbolInformation] = symbolTable.info(symbol).filter {
      _.kind match {
        case Kind.TRAIT | Kind.TYPE | Kind.CLASS | Kind.OBJECT => true
        case _                                                 => false
      }
    }

    def isTypeParam(symbol: Symbol): Boolean = typeParamSymInfo(symbol).isDefined

    def isClass(symbol: Symbol) = classSymInfo(symbol).isDefined

    def isType(symbol: Symbol) = typeSymInfo(symbol).isDefined

  }

  implicit class ClassSignatureOps(val classSignature: ClassSignature) extends AnyVal {
    def typeParamSymbols: Seq[String]                           = classSignature.typeParameters.typeParamSymbols
    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = classSignature.typeParameters.typeParamDisplayNames(symTab)
  }

  implicit class TypeOps(val tpe: isb.Type) extends AnyVal {

    def isTypeParam(symTab: SymbolTable): Boolean = typeSymbol.map(symTab.isTypeParam(_)).getOrElse(false)

    def typeSymbol: Option[String] = tpe match {
      case TypeRef(isb.Type.Empty, symbol, _) => Some(symbol)
      case SingleType(isb.Type.Empty, symbol) => Some(symbol)
      case _                                  => None
    }

    def parents(symTab: SymbolTable): Seq[isb.Type] = tpe match {
      case IntersectionType(types)                => types.flatMap(_.parents(symTab))
      case SuperType(prefix, symbol)              => symTab.info(symbol).toSeq.flatMap(_.parents(symTab))
      case ByNameType(tpe)                        => tpe.parents(symTab)
      case AnnotatedType(annotations, tpe)        => tpe.parents(symTab)
      case TypeRef(prefix, symbol, typeArguments) => symTab.info(symbol).toSeq.flatMap(_.parents(symTab))
      case StructuralType(tpe, declarations)      => tpe.parents(symTab)
      case ConstantType(constant)                 => Seq()
      case ThisType(symbol)                       => symTab.info(symbol).toSeq.flatMap(_.parents(symTab))
      case RepeatedType(tpe)                      => tpe.parents(symTab)
      case WithType(types)                        => types.flatMap(_.parents(symTab))
      case UniversalType(typeParameters, tpe)     => tpe.parents(symTab)
      case SingleType(prefix, symbol)             => symTab.info(symbol).toSeq.flatMap(_.parents(symTab))
      case ExistentialType(tpe, declarations)     => tpe.parents(symTab)
      case UnionType(types)                       => types.flatMap(_.parents(symTab))
      case isb.Type.Empty                         => Seq()
    }

    def ancestors(symTab: SymbolTable): Seq[isb.Type] = tpe match {
      case IntersectionType(types)                => types.flatMap(_.ancestors(symTab))
      case SuperType(prefix, symbol)              => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
      case ByNameType(tpe)                        => tpe.ancestors(symTab)
      case AnnotatedType(annotations, tpe)        => tpe.ancestors(symTab)
      case TypeRef(prefix, symbol, typeArguments) => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
      case StructuralType(tpe, declarations)      => tpe.ancestors(symTab)
      case ConstantType(constant)                 => Seq()
      case ThisType(symbol)                       => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
      case RepeatedType(tpe)                      => tpe.ancestors(symTab)
      case WithType(types)                        => types.flatMap(_.ancestors(symTab))
      case UniversalType(typeParameters, tpe)     => tpe.ancestors(symTab)
      case SingleType(prefix, symbol)             => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
      case ExistentialType(tpe, declarations)     => tpe.ancestors(symTab)
      case UnionType(types)                       => types.flatMap(_.ancestors(symTab))
      case isb.Type.Empty                         => Seq()
    }

    def ancestorsOrSelf(symTab: SymbolTable): Seq[isb.Type] =
      tpe +: (tpe match {
        case IntersectionType(types)                => types.flatMap(_.ancestors(symTab))
        case SuperType(prefix, symbol)              => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
        case ByNameType(tpe)                        => tpe.ancestors(symTab)
        case AnnotatedType(annotations, tpe)        => tpe.ancestors(symTab)
        case TypeRef(prefix, symbol, typeArguments) => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
        case StructuralType(tpe, declarations)      => tpe.ancestors(symTab)
        case ConstantType(constant)                 => Seq()
        case ThisType(symbol)                       => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
        case RepeatedType(tpe)                      => tpe.ancestors(symTab)
        case WithType(types)                        => types.flatMap(_.ancestors(symTab))
        case UniversalType(typeParameters, tpe)     => tpe.ancestors(symTab)
        case SingleType(prefix, symbol)             => symTab.info(symbol).toSeq.flatMap(_.ancestors(symTab))
        case ExistentialType(tpe, declarations)     => tpe.ancestors(symTab)
        case UnionType(types)                       => types.flatMap(_.ancestors(symTab))
        case isb.Type.Empty                         => Seq()
      })

    /**
      * Checks if this type is a subtype of the given symbol
      */
    def isSubtypeOf(sym: Symbol, symTab: SymbolTable): Boolean = {
      tpe.typeSymbol.exists(_ == sym) || tpe.parents(symTab).exists(_.isSubtypeOf(sym, symTab))
    }

  }

  implicit class SymbolInformationOps(val si: SymbolInformation) extends AnyVal {

    def parents(symTab: SymbolTable): Seq[isb.Type] =
      if (si.signature.isInstanceOf[ClassSignature]) {
        si.signature.asInstanceOf[ClassSignature].parents
      } else if (si.signature.isInstanceOf[TypeSignature]) {
        val ts = si.signature.asInstanceOf[TypeSignature]
        ts.lowerBound.parents(symTab) ++ ts.upperBound.parents(symTab)
      } else {
        Seq()
      }

    def ancestors(symTab: SymbolTable): Seq[isb.Type] =
      if (si.signature.isInstanceOf[ClassSignature]) {
        si.signature.asInstanceOf[ClassSignature].parents.flatMap(_.ancestorsOrSelf(symTab))
      } else if (si.signature.isInstanceOf[TypeSignature]) {
        val ts = si.signature.asInstanceOf[TypeSignature]
        ts.lowerBound.ancestors(symTab) ++ ts.upperBound.ancestors(symTab)
      } else {
        Seq()
      }

    /**
      * Checks if this symbol information is a subtype of the given symbol
      */
    def isSubtypeOf(sym: Symbol, symTab: SymbolTable): Boolean = {
      si.symbol == sym || si.parents(symTab).exists(_.isSubtypeOf(sym, symTab))
    }

    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = {
      if (si.signature.isInstanceOf[ClassSignature]) {
        si.signature.asInstanceOf[isb.ClassSignature].typeParameters.typeParamDisplayNames(symTab)
      } else if (si.signature.isInstanceOf[TypeSignature]) {
        si.signature.asInstanceOf[isb.TypeSignature].typeParameters.typeParamDisplayNames(symTab)
      } else if (si.signature.isInstanceOf[MethodSignature]) {
        si.signature.asInstanceOf[isb.MethodSignature].typeParameters.typeParamDisplayNames(symTab)
      } else {
        Seq()
      }
    }

    def parentTypes(symTab: SymbolTable): Seq[ParentType] = {
      si.parents(symTab).collect {
        case TypeRef(isb.Type.Empty, symbol, typeArguments)  =>
          ParentType(FullName.fromSymbol(symbol), typeArguments)
      }
    }

  }

  implicit class ScopeOptionOps(val o: Option[Scope]) {
    def typeParamSymbols: Seq[String] = o.toSeq.flatMap(_.symlinks)
    def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = typeParamSymbols.map(symTab.info(_).get.displayName)
  }

  type Symbol = String

  // a type formatter that can format some types (but maybe not all)
  type PTypeFormatter = PartialFunction[isb.Type, String]

  // creator for a partial type formatter
  // -> the creator can use the given type formatter to delegate formatting of nested types
  type CTypeFormatter = TypeFormatter => PTypeFormatter

}
