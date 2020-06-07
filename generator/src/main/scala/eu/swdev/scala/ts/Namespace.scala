package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, ConstantType, SingleType, SymbolInformation}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

class Namespace(val name: SimpleName) {

  import Namespace._

  val nested  = mutable.SortedMap.empty[SimpleName, Namespace]
  val itfs    = mutable.SortedMap.empty[SimpleName, Interface]
  val unions  = mutable.SortedMap.empty[SimpleName, Union]
  val aliases = mutable.SortedMap.empty[SimpleName, Alias]

  def +=(itf: Interface): this.type = {
    enclosingNamespace(itf.fullName).itfs += itf.simpleName -> itf
    this
  }

  def +=(union: Union): this.type = {
    enclosingNamespace(union.fullName).unions += union.fullName.last -> union
    this
  }

  def +=(alias: Alias): this.type = {
    enclosingNamespace(alias.fullName).aliases += alias.fullName.last -> alias
    this
  }

  def containsItf(fullName: FullName): Boolean = contains(fullName, spaceContainsItf)

  def containsItfOrType(fullName: FullName): Boolean = contains(fullName, spaceContainsItfOrAlias)

  private def contains(fullName: FullName, p: (Namespace, SimpleName) => Boolean): Boolean = {
    val sn = fullName.head
    fullName.tail match {
      case Some(fn) => nested.get(sn).map(_.contains(fn, p)).getOrElse(false)
      case None     => p(this, sn)
    }
  }

  private def enclosingNamespace(fullName: FullName): Namespace = {
    var n = this
    fullName.str.split('.').dropRight(1).map(SimpleName(_)).foreach { p =>
      n = n.nested.getOrElseUpdate(p, new Namespace(p))
    }
    n
  }

}

object Namespace {

  def spaceContainsItf(namespace: Namespace, name: SimpleName)        = namespace.itfs.contains(name)
  def spaceContainsItfOrAlias(namespace: Namespace, name: SimpleName) = namespace.itfs.contains(name) || namespace.aliases.contains(name)

  def deriveInterfaces(inputs: List[Input.TopLevel], symTab: SymbolTable): Namespace = {

    val rootNamespace = new Namespace(SimpleName(""))

    // all symbols that are referenced (directly or transitively) by the top level exported api
    val apiRefs = ReferencedSymbolsAnalyzer.referencedSymbols(inputs, symTab)

    // determine the ancestors of all types that are referenced in the API
    val ancestorRefs = inputs
      .collect {
        case i: Input.Type if apiRefs.contains(i.si.symbol) => i.si.ancestors(symTab).flatMap(_.typeSymbol)
      }
      .flatten
      .toSet

    // add interfaces for all types in the input that are referenced
    // -> classes and object that are top level exports are not added here
    // -> they are added right beside their class / const during code generation
    inputs.filter(i => apiRefs.contains(i.si.symbol) || ancestorRefs.contains(i.si.symbol)).foreach {
      case i: Input.Cls if !i.name.isDefined => rootNamespace += Interface(i.si, i.member, symTab)
      case i: Input.Obj if !i.name.isDefined => rootNamespace += Interface(i.si, i.member, symTab)
      case i: Input.Trait                    => rootNamespace += Interface(i.si, i.member, symTab)
      case i: Input.Alias                    => rootNamespace += Alias(i)
      case _                                 =>
    }

    // topLevelSymbols that have already been added
    // -> the namespace.containsItfOrType test does not cover top level symbols because they are placed in the root
    //    namespace and not in namespace corresponding to their package
    val topLevelSymbols = inputs.collect {
      case i: Input.Cls if i.name.isDefined => i.si.symbol
      case i: Input.Obj if i.name.isDefined => i.si.symbol
    }.toSet

    def isAlreadyAdded(sym: Symbol) = topLevelSymbols.contains(sym) || rootNamespace.containsItfOrType(FullName(sym))

    def canBeAdded(sym: Symbol) = !(isAlreadyAdded(sym) || BuiltIn.builtInTypeNames.contains(sym) || isSpecialType(sym))

    // add interfaces for all types that are referenced but have no interface yet
    val referenced = apiRefs
      .flatMap(symTab.typeSymInfo)
      .collect {
        case si if canBeAdded(si.symbol) => Interface(si, symTab)
      }

    referenced.foreach(rootNamespace += _)

    // close the gaps in the inheritance hierarchy
    // -> add interfaces for all ancestors that have an already exported ancestor
    val gaps = ancestorRefs.flatMap(symTab.typeSymInfo(_)).collect {
      case si if canBeAdded(si.symbol) && si.ancestors(symTab).flatMap(_.typeSymbol).exists(isAlreadyAdded) => Interface(si, symTab)
    }

    gaps.foreach(rootNamespace += _)

    rootNamespace
  }

  def isSpecialType(symbol: Symbol) =
    specialTypes.contains(symbol) ||
      symbol.matches("scala/scalajs/js/Function\\d+#") ||
      symbol.matches("scala/scalajs/js/Tuple\\d+#")

  val specialTypes = Set[Symbol]("scala/scalajs/js/package.UndefOr#", "scala/scalajs/js/Array#", "scala/scalajs/js/`|`#")

}
