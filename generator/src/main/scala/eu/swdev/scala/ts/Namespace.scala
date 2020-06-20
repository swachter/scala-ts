package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, ConstantType, SingleType, SymbolInformation}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

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

  def deriveInterfaces(inputs: List[Input.Defn], symTab: SymbolTable): Namespace = {

    val rootNamespace = new Namespace("")

    // all symbols that are referenced (directly or transitively) by the top level exported api
    val apiRefs = ReferencedSymbolsAnalyzer.referencedSymbols(inputs, symTab)

    val allTypes = Analyzer.types(inputs)

    // the types that are referenced in the exported API
    val referencedTypes = allTypes.filter(tpe => apiRefs.contains(tpe.si.symbol))

    // determine the type symbols of all ancestors of referenced types
    val ancestorRefs = referencedTypes.flatMap(_.si.ancestors(symTab)).flatMap(_.typeSymbol).toSet

    // add interfaces for all types in the input that are referenced
    // -> classes and object that are top level exports are not added here
    // -> they are added right beside their class / const during code generation
    allTypes.filter(i => apiRefs.contains(i.si.symbol) || ancestorRefs.contains(i.si.symbol)).foreach {
      case i: Input.Cls if !i.isTopLevelExport => rootNamespace += Output.Interface(i.si, i.member ++ i.ctorParams, symTab)
      case i: Input.Obj if !i.isTopLevelExport => rootNamespace += Output.Interface(i.si, i.member, symTab)
      case i: Input.Trait                      => rootNamespace += Output.Interface(i.si, i.member, symTab)
      case i: Input.Alias                      => rootNamespace += Output.Alias(i)
      case _: Input.Cls | _: Input.Obj         =>
    }

    // topLevelSymbols that have already been added
    // -> the namespace.containsItfOrType test does not cover top level symbols because they are placed in the root
    //    namespace and not in namespace corresponding to their package
    val topLevelSymbols = allTypes.collect {
      case i: Input.Cls if i.isTopLevelExport => i.si.symbol
      case i: Input.Obj if i.isTopLevelExport => i.si.symbol
    }.toSet

    def isAlreadyAdded(sym: Symbol) = topLevelSymbols.contains(sym) || rootNamespace.contains(FullName.fromSymbol(sym))

    def canBeAdded(sym: Symbol) = !(isAlreadyAdded(sym) || BuiltIn.builtInTypeNames.contains(sym) || isSpecialType(sym))

    // add interfaces for all types that are referenced but have no interface yet
    val referenced = apiRefs
      .flatMap(symTab.typeSymInfo)
      .collect {
        case si if canBeAdded(si.symbol) => Output.Interface(si, symTab)
      }

    referenced.foreach(rootNamespace += _)

    // close the gaps in the inheritance hierarchy
    // -> add interfaces for all ancestors that have an already exported ancestor
    val gaps = ancestorRefs.flatMap(symTab.typeSymInfo(_)).collect {
      case si if canBeAdded(si.symbol) && si.ancestors(symTab).flatMap(_.typeSymbol).exists(isAlreadyAdded) => Output.Interface(si, symTab)
    }

    gaps.foreach(rootNamespace += _)

    rootNamespace
  }

  def isSpecialType(symbol: Symbol) =
    specialTypes.contains(symbol) ||
      symbol.matches("scala/scalajs/js/Function\\d+#") ||
      symbol.matches("scala/scalajs/js/ThisFunction\\d+#") ||
      symbol.matches("scala/scalajs/js/Tuple\\d+#")

  val specialTypes = Set[Symbol](
    "scala/scalajs/js/`|`#",
    "scala/scalajs/js/package.UndefOr#",
    "scala/scalajs/js/Array#",
    "scala/scalajs/js/Date#",
    "scala/scalajs/js/Dictionary#",
    "scala/scalajs/js/Iterable#",
    "scala/scalajs/js/Iterator#",
    "scala/scalajs/js/Promise#",
    "scala/scalajs/js/RegExp#",
    "scala/scalajs/js/Symbol#",
  )

}
