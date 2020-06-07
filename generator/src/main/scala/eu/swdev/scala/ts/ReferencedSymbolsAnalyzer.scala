package eu.swdev.scala.ts

import scala.collection.mutable
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
  Signature,
  SingleType,
  StructuralType,
  SuperType,
  SymbolInformation,
  ThisType,
  Type,
  TypeRef,
  TypeSignature,
  UnionType,
  UniversalType,
  ValueSignature,
  WithType
}
import scala.meta.internal.symtab.SymbolTable

object ReferencedSymbolsAnalyzer {

  /**
    * Determine all type symbols that are referenced when recursively traversing the top level exported declarations.
    *
    * The symbols of the top level exported items are also included.
    */
  def referencedSymbols(inputs: List[Input], symTab: SymbolTable): Set[Symbol] = {

    val topLevelExports = inputs.collect {
      case i: Input.Cls if i.name.isDefined => i // classes are considered only if they have a @JSTopLevelExport
      case i: Input.Obj if i.name.isDefined => i // objects are considered only if they have a @JSTopLevelExport
      case i: Input.Member                  => i
    }

    val c = new Collector(inputs, symTab, mutable.Set.empty)
    topLevelExports.foreach(i => c.collect(i.si))
    c.accu.filter(symTab.isType(_)).toSet
  }

  class Collector(inputs: List[Input], symTab: SymbolTable, val accu: mutable.Set[Symbol]) {

    val inputTypes = inputs.collect { case e: Input.Type => e.si.symbol -> e }.toMap

    def add(sym: Symbol)(goOn: => Unit): Unit = if (accu.add(sym)) {
      inputTypes.get(sym) match {
        case Some(inputType) => collectMember(inputType)
        case None            => goOn
      }
    }

    def collect(si: SymbolInformation): Unit = {
      add(si.symbol) {
        collect(si.signature)
      }
    }

    private def collectMember(i: Input.Type): Unit = i match {
      case i @ Input.Obj(semSrc, tree, name, si, member)             => member.foreach(collect)
      case i @ Input.Cls(semSrc, tree, name, si, member, ctorParams) => member.foreach(collect); ctorParams.foreach(collect)
      case i @ Input.Trait(semSrc, tree, si, member)                 => member.foreach(collect)
      case i @ Input.Alias(semSrc, tree, si)                         => collect(i.typeSignature)
    }

    def collect(i: Input.Member): Unit = collect(i.methodSignature)

    def collect(i: Input.CtorParam): Unit = collect(i.si)

    def collect(sig: Signature): Unit = sig match {
      case ValueSignature(tpe)                                   => collect(tpe)
      case TypeSignature(typeParameters, lowerBound, upperBound) =>
        // do not collect bound limits
        (lowerBound.typeSymbol, upperBound.typeSymbol) match {
          case (Some("scala/Nothing#"), Some("scala/Any#")) =>
          case (Some("scala/Nothing#"), _)                  => collect(upperBound)
          case (_, Some("scala/Any#"))                      => collect(lowerBound)
          case _                                            => collect(lowerBound); collect(upperBound)
        }
      case ClassSignature(typeParameters, parents, self, declarations) => // do not look inside the class -> it was not in the source
      case Signature.Empty                                             =>
      case MethodSignature(typeParameters, parameterLists, returnType) => parameterLists.foreach(collect); collect(returnType)
    }

    def collect(tpe: Type): Unit = tpe match {
      case IntersectionType(types)                => types.foreach(collect)
      case SuperType(prefix, symbol)              => symTab.info(symbol).foreach(collect)
      case ByNameType(tpe)                        => collect(tpe)
      case AnnotatedType(annotations, tpe)        => collect(tpe)
      case TypeRef(prefix, symbol, typeArguments) => symTab.info(symbol).foreach(collect); typeArguments.foreach(collect)
      case StructuralType(tpe, declarations)      => collect(tpe)
      case ConstantType(constant)                 =>
      case ThisType(symbol)                       => symTab.info(symbol).foreach(collect)
      case RepeatedType(tpe)                      => collect(tpe)
      case WithType(types)                        => types.foreach(collect)
      case UniversalType(typeParameters, tpe)     => collect(tpe)
      case SingleType(prefix, symbol)             => symTab.info(symbol).foreach(collect)
      case ExistentialType(tpe, declarations)     => collect(tpe)
      case UnionType(types)                       => types.foreach(collect)
      case Type.Empty                             =>
    }

    def collect(o: Option[Scope]): Unit = o.foreach { s =>
      s.symlinks.foreach(symTab.info(_).foreach(collect))
      s.hardlinks.foreach(collect)
    }

    def collect(s: Scope): Unit = {
      s.symlinks.foreach(symTab.info(_).foreach(collect))
      s.hardlinks.foreach(collect)
    }

  }

}
