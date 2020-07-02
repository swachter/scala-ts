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
  def referencedTypeSymbols(inputs: List[Input.Defn], symTab: SymbolTable): Set[Symbol] = {

    val topLevelExports = Analyzer.topLevel(inputs).map(_.exportable)

    val c = new Collector(inputs, symTab)
    topLevelExports.foreach(i => c.collect(i.si))
    c.accu.filter(symTab.isType(_)).toSet
  }

  class Collector(inputs: List[Input], symTab: SymbolTable) {

    // symbols of all visited definitions (vals, vars, defs, traits, classes, or type aliases)
    val accu = mutable.Set.empty[Symbol]

    val inputTypes = inputs.collect { case e: Input.Type => e.si.symbol -> e }.toMap

    def collect(si: SymbolInformation): Unit = {
      val sym = si.symbol
      if (accu.add(sym)) {
        inputTypes.get(sym) match {
          case Some(inputType) => collectMembers(inputType)
          case None            => collect(si.signature)
        }
      }
    }

    // collect the types that are referenced in the export by a known input type
    private def collectMembers(i: Input.Type): Unit = i match {
      case i: Input.Obj   => i.member.foreach(collectExportedMember)
      case i: Input.Cls   => i.member.foreach(collectExportedMember); i.ctorParams.foreach(cp => collect(cp.si))
      case i: Input.Trait => i.member.foreach(collectExportedMember)
      case i: Input.Alias => collect(i.typeSignature)
    }

    def collectExportedMember(i: Input.Defn): Unit = i match {
      case i: Input.DefOrValOrVar             => collect(i.methodSignature) // all Input.DefOrValOrVar are exported
      case i: Input.Obj if i.isExportedMember => collect(i.si) // only some Input.Obj are exported as members
      case i: Input.Type                      => // nested type members are not considered; they do not appear as members in the output
    }

    def collect(sig: Signature): Unit = sig match {
      case ValueSignature(tpe)                                   => collect(tpe)
      case TypeSignature(typeParameters, lowerBound, upperBound) =>
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
