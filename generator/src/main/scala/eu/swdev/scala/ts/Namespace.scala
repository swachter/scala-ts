package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.{ClassSignature, ConstantType, SingleType}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

class Namespace(val name: SimpleName) {

  val nested = mutable.SortedMap.empty[SimpleName, Namespace]
  val itfs   = mutable.SortedMap.empty[SimpleName, Interface]
  val unions = mutable.SortedMap.empty[SimpleName, Union]
  val types  = mutable.SortedMap.empty[SimpleName, TypeAlias]

  def +=(itf: Interface): this.type = {
    enclosingNamespace(itf.fullName).itfs += itf.simpleName -> itf
    this
  }

  def +=(union: Union): this.type = {
    enclosingNamespace(union.fullName).unions += union.fullName.last -> union
    this
  }

  def +=(typeAlias: TypeAlias): this.type = {
    enclosingNamespace(typeAlias.fullName).types += typeAlias.fullName.last -> typeAlias
    this
  }

  def containsItf(fullName: FullName): Boolean = contains(fullName, itfs.contains)

  def containsItfOrType(fullName: FullName): Boolean = contains(fullName, sn => itfs.contains(sn) || types.contains(sn))

  private def contains(fullName: FullName, p: SimpleName => Boolean): Boolean = {
    val sn = fullName.head
    fullName.tail match {
      case Some(fn) => nested.get(sn).map(_.contains(fn, p)).getOrElse(false)
      case None     => p(sn)
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

  def deriveInterfaces(exports: List[Export.TopLevel], symTab: SymbolTable): Namespace = {

    val rootNamespace = new Namespace(SimpleName(""))

    val rootSymbols = exports.collect {
      case e: Export.Cls => e.si.symbol
      case e: Export.Obj => e.si.symbol
    }.toSet

    // collect the symbols of all exported classes / objects and their ancestors
    val allSymbols = Namespace.ancestorsOrSelf(rootSymbols.iterator, mutable.Set.empty, symTab)

    // generate interfaces for all traits that are in the ancestors of exported classes / objects
    val traitInterfaces = exports.collect {
      case e: Export.Trt if allSymbols.contains(e.si.symbol) => Interface(e, symTab)
    }
    traitInterfaces.foreach(rootNamespace += _)

    val traitInterfaceSymbols = traitInterfaces.map(_.symbol).toSet

    // collect all types that are referenced in defs, vals, vars, or constructor params
    val referencedTypes = exports.collect {
      // ignore traits for which no interface was created
      case e: Export.Trt if !allSymbols.contains(e.si.symbol) => Seq.empty
      case e                                                  => Analyzer.referencedTypes(e, symTab)
    }.flatten

    val referencedTypeSymbols = referencedTypes.flatMap(_.typeSymbol).toSet

    val typeAliases = exports.collect {
      case e: Export.Tpe if referencedTypeSymbols.contains(e.si.symbol) => TypeAlias(e)
    }

    typeAliases.foreach(rootNamespace += _)

    val typeAliasSymbols = typeAliases.map(_.e.si.symbol).toSet

    // checks if the given type is an exported class / object, an exported trait interface or a built-in or special type
    def isKnownType(tpe: isb.Type): Boolean = tpe match {
      case ConstantType(_) => true
      case _ =>
        tpe.typeSymbol match {
          case Some(symbol) => isKnownTypeSymbol(symbol)
          case _            => false
        }
    }

    def isKnownTypeSymbol(symbol: Symbol): Boolean = {
      rootSymbols.contains(symbol) ||
      traitInterfaceSymbols.contains(symbol) ||
      typeAliasSymbols.contains(symbol) ||
      BuiltIn.builtInTypeNames.contains(symbol) ||
      isSpecialType(symbol)
    }

    // create interfaces for all referenced types that do not correspond to exported class / object / trait or are
    // otherwise known
    val referencedInterfaces = referencedTypes
      .collect {
        case tpe if !isKnownType(tpe) => tpe.typeSymbol.flatMap(symTab.info(_)).map(Interface(_, symTab))
      }
      .collect {
        case Some(s) => s
      }

    referencedInterfaces.foreach(rootNamespace += _)

    // create interfaces for all types between exported classes / objects and already created interfaces
    // -> fill the gaps in inheritance chains

    def hasExportedAncestorInterface(sym: Symbol): Boolean = {
      symTab.info(sym) match {
        case Some(si) => si.parents.exists(hasExportedAncestorOrSelfInterface(_))
        case None     => false
      }
    }

    def hasExportedAncestorOrSelfInterface(tpe: isb.Type): Boolean = {
      tpe.typeSymbol match {
        case Some(sym) => rootNamespace.containsItf(fullName(sym)) || hasExportedAncestorInterface(sym)
        case None      => false
      }
    }

    val intermediateInterfaces = allSymbols
      .filter(!isKnownTypeSymbol(_))
      .filter(hasExportedAncestorInterface(_))
      .collect {
        case sym => symTab.info(sym).map(Interface(_, symTab))
      }
      .collect {
        case Some(s) => s
      }

    intermediateInterfaces.foreach(rootNamespace += _)

    rootNamespace
  }

  // collects all symbols of the given children and their ancestors
  def ancestorsOrSelf(children: Iterator[Symbol], accu: mutable.Set[Symbol], symTab: SymbolTable): mutable.Set[Symbol] = {
    if (children.hasNext) {
      val sym = children.next()
      if (accu.contains(sym)) {
        ancestorsOrSelf(children, accu, symTab)
      } else {
        val parentSymbols = symTab
          .info(sym)
          .toSeq
          .flatMap(_.parents)
          .flatMap(_.typeSymbol)
        val a = ancestorsOrSelf(parentSymbols.iterator, accu += sym, symTab)
        ancestorsOrSelf(children, a, symTab)
      }
    } else {
      accu
    }
  }

  def isSpecialType(symbol: Symbol) =
    specialTypes.contains(symbol) ||
      symbol.matches("scala/scalajs/js/Function\\d+#") ||
      symbol.matches("scala/scalajs/js/Tuple\\d+#")

  val specialTypes = Set[Symbol]("scala/scalajs/js/package.UndefOr#", "scala/scalajs/js/Array#", "scala/scalajs/js/`|`#")

}
