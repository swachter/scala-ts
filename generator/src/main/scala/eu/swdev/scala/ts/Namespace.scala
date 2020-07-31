package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.symtab.SymbolTable

class Namespace(val name: String) {

  val nested = mutable.SortedMap.empty[String, Namespace]
  val types  = mutable.SortedMap.empty[String, Output.Type]

  def +=(tpe: Output.Type): this.type = {
    enclosingNamespace(tpe.fullName).types += tpe.simpleName -> tpe
    this
  }

  def contains(fullName: FullName): Boolean = {
    val sn = fullName.head
    fullName.tail match {
      case Some(fn) => nested.get(sn).map(_.contains(fn)).getOrElse(false)
      case None     => types.contains(sn)
    }
  }

  private def enclosingNamespace(fullName: FullName): Namespace = {
    var n = this
    fullName.str.split('.').dropRight(1).foreach { p =>
      n = n.nested.getOrElseUpdate(p, new Namespace(p))
    }
    n
  }

}

object Namespace {

  /**
    * Constructs a namespace filled with interfaces and type aliases based in the given inputs.
    *
    * Interfaces for exported classes / objects are not included.
    */
  def apply(inputs: Inputs, symTab: SymbolTable, isKnownOrBuiltIn: Symbol => Boolean, nativeAnalyzer: NativeSymbolAnalyzer): Namespace = {

    val rootNamespace = new Namespace("")

    // all type symbols that are referenced (directly or transitively) by the top level exported api
    val apiRefs = ReferencedSymbolsAnalyzer.referencedTypeSymbols(inputs, symTab)

    val allInputTypes = Analyzer.types(inputs)

    // the types that are referenced in the exported API
    val referencedTypes = allInputTypes.filter(tpe => apiRefs.contains(tpe.si.symbol))

    // determine the type symbols of all ancestors of referenced types
    val ancestorRefs = referencedTypes.flatMap(_.si.ancestors(symTab)).flatMap(_.typeSymbol(symTab)).toSet

    // types referenced by the API or ancestors thereof
    val referencedOrAncestorTypes = allInputTypes.filter(i => apiRefs.contains(i.si.symbol) || ancestorRefs.contains(i.si.symbol))

    // maps type symbols of those types that are imported from global scope or from a module into their Global/Imported information
    val importedOrGlobalSymbols = apiRefs
      .map(sym => sym -> nativeAnalyzer.nativeSymbol(sym))
      .collect {
        case (s, Some(n: NativeSymbol)) if !isExported(n) => s -> n
      }
      .toMap

    // no interfaces must be added for
    // - top level exports (their declarations are added later on in the generator)
    // - class / objects that are from global scope or imported
    def mustAddInterface(i: Input.ClsOrObj) = !(i.isTopLevelExport || importedOrGlobalSymbols.contains(i.si.symbol))

    // add interfaces and type aliases for all referenced types or ancestor types that are not top level / global scope / imported
    // -> classes and object that are top level exports are not added here
    // -> they are added right beside their class / const during code generation
    referencedOrAncestorTypes.foreach {
      case i: Input.Cls if mustAddInterface(i) => rootNamespace += Output.Interface(i.si, i.member ++ i.ctorParams, symTab)
      case i: Input.Obj if mustAddInterface(i) => rootNamespace += Output.Interface(i.si, i.member, symTab)
      case i: Input.Trait                      => rootNamespace += Output.Interface(i.si, i.member, symTab)
      case i: Input.Alias                      => rootNamespace += Output.Alias(i)
      case _: Input.Cls | _: Input.Obj         =>
    }

    // symbols of top level exports
    // -> the namespace.contains test does not cover top level symbols because they are placed in the root
    //    namespace and not in namespace corresponding to their package
    val topLevelSymbols = allInputTypes.collect {
      case i: Input.Cls if i.isTopLevelExport => i.si.symbol
      case i: Input.Obj if i.isTopLevelExport => i.si.symbol
    }.toSet

    def isExportedOrNamedNative(sym: Symbol) =
      topLevelSymbols.contains(sym) || rootNamespace.contains(FullName.fromSymbol(sym)) || importedOrGlobalSymbols.contains(sym)

    def canBeAdded(sym: Symbol) = !(isExportedOrNamedNative(sym) || isKnownOrBuiltIn(sym))

    // add interfaces for all types that are referenced but have no interface yet
    val referenced = apiRefs
      .flatMap(symTab.typeSymInfo)
      .collect {
        case si if canBeAdded(si.symbol) => Output.Interface(si, symTab)
      }

    referenced.foreach(rootNamespace += _)

    // close the gaps in the inheritance hierarchy
    // -> add interfaces for all ancestors that have an already exported ancestor
    ancestorRefs.flatMap(symTab.typeSymInfo(_)).collect {
      case si if canBeAdded(si.symbol) && si.ancestors(symTab).flatMap(_.typeSymbol(symTab)).exists(isExportedOrNamedNative) =>
        rootNamespace += Output.Interface(si, symTab)
    }

    rootNamespace
  }

  def isExported(n: NativeSymbol): Boolean = n match {
    case NativeSymbol.Exported(_, _)     => true
    case NativeSymbol.Inner(_, outer, _) => isExported(outer)
    case _                               => false
  }
}
