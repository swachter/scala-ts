package eu.swdev.scala.ts

import java.util.StringTokenizer

import scala.reflect.runtime.{universe => ru}
import ru._
import scala.meta.internal.semanticdb.{ClassSignature, MethodSignature, SymbolInformation, TypeSignature, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.reflect.NameTransformer
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Analyzes if symbols are native, i.e. if they are from global scope, imported, or exported.
  *
  * Annotation values are not stored in semantic db. Therefore scala-reflect is used to access these.
  */
class NativeSymbolAnalyzer(topLevelExports: Map[String, NativeSymbol.Exported], cl: ClassLoader, symTab: SymbolTable) {

  private val mirror = ru.runtimeMirror(cl)

  private val nativeSymbols = mutable.Map.empty[String, Option[NativeSymbol]]

  nativeSymbols ++= topLevelExports.mapValues(Option(_))

  def nativeSymbol(sym: String): Option[NativeSymbol] = synchronized {
    nativeSymbols.getOrElseUpdate(
      sym,
      if (sym == "scala/AnyRef#") {
        None
      } else {
        symTab.info(sym).flatMap { si =>
          si.signature match {
            case TypeSignature(_, _, upperBound) => upperBound.typeSymbol.flatMap(s => nativeSymbol(s)) // follow type aliases
            case ClassSignature(_, _, _, _)      => nativeClassSymbol(si)
            case MethodSignature(_, _, _)        => nativeMethodSymbol(si) // defs, vals, and vars
            case _                               => None
          }
        }
      }
    )
  }

  def nativeSymbolImports: Iterable[(String, String)] = synchronized {
    nativeSymbols.values
      .collect {
        case Some(s) => s
      }
      .flatMap(nativeSymbolImport)
  }

  private def nativeSymbolImport(ns: NativeSymbol): Seq[(String, String)] = ns match {
    case NativeSymbol.ImportedName(module, name, _) => Seq(module -> name)
    case NativeSymbol.ImportedNamespace(module, _)  => Seq(module -> "")
    case NativeSymbol.Inner(_, outer, _)            => nativeSymbolImport(outer)
    case _                                          => Seq()
  }

  private def nativeClassSymbol(si: SymbolInformation): Option[NativeSymbol] = {
    outerNativeClassSymbol(si).orElse(innerNativeClassSymbol(si))
  }

  private def nativeMethodSymbol(si: SymbolInformation): Option[NativeSymbol] = {
    val splitted      = si.symbol.split('/')
    val pckg          = splitted.dropRight(1).mkString("/")
    val nested        = new StringTokenizer(splitted.last, ".#", true).asScala.grouped(2).map(_.mkString("")).toList
    val owner         = nested.dropRight(1).mkString("")
    val ownerFullName = s"$pckg/$owner"
    val tApi          = symbolInformation2TypeSymbolApi(ownerFullName)

    val last      = nested.last
    val innerName = transform(last.dropRight(1))

    val mApi = tApi.toType.decls.collectFirst {
      case m: MethodSymbolApi if m.name.toString == innerName => m
    }.get

    val defaultName = mApi.name.toString
    val as          = mApi.annotations

    val outerNativeMethodSymbol = checkGlobal(si, as, defaultName).orElse(checkImported(si, as)).orElse(checkExported(si))

    outerNativeMethodSymbol.orElse {
      symTab.info(ownerFullName).flatMap(nativeClassSymbol).map { ownerNativeSymbol =>
        val in = checkName(as).orElse(checkExport(as)).getOrElse(innerName)
        NativeSymbol.Inner(in, ownerNativeSymbol, false)
      }
    }

  }

  // transforms a Scalameta symbol into a scala-reflect symbol
  private def transform(sym: String): String = NameTransformer.encode(sym.replace("`", ""))

  private def symbolInformation2TypeSymbolApi(si: SymbolInformation): TypeSymbolApi = symbolInformation2TypeSymbolApi(si.symbol)

  private def symbolInformation2TypeSymbolApi(sym: String): TypeSymbolApi = {

    val splitted = sym.split('/')
    val pckg     = splitted.dropRight(1).filter(_ != "_empty_")
    val nested   = new StringTokenizer(splitted.last, ".#", true).asScala.grouped(2).map(_.mkString("")).toList

    val objPrefix = nested.takeWhile(_.endsWith("."))
    val clsSuffix = nested.dropWhile(_.endsWith("."))

    // the first class from the clsSufix is accessible statically
    // -> append it to the prefix and remove it from the suffix
    val (prefix, suffix) = clsSuffix match {
      case head :: tail => (objPrefix :+ head, tail)
      case _            => (objPrefix, Nil)
    }

    // calculate the outer name of the statically accessible prefix
    val outerName     = prefix.map(s => transform(s.dropRight(1))).mkString(".")
    val fullOuterName = (pckg :+ outerName).mkString(".")
    var tApi: TypeSymbolApi = if (prefix.last.endsWith(".")) {
      mirror.staticModule(fullOuterName).moduleClass.asClass
    } else {
      mirror.staticClass(fullOuterName)
    }

    // iteratively handle the suffix by navigating into nested classes and objects
    var tail = suffix
    while (tail.nonEmpty) {
      val head      = tail.head
      val innerName = transform(head.dropRight(1))
      if (head.endsWith(".")) {
        val o = tApi.toType.decls.find(sym => sym.isModule && sym.name.decoded == innerName)
        tApi = o.get.asModule.moduleClass.asClass
      } else {
        val o = tApi.toType.decls.find(sym => sym.isClass && sym.name.decoded == innerName)
        tApi = o.get.asClass
      }
      tail = tail.tail
    }

    tApi
  }

  private def innerNativeClassSymbol(si: SymbolInformation): Option[NativeSymbol.Inner] = {
    val idx = if (si.symbol.endsWith(".")) si.symbol.lastIndexOf('.', si.symbol.length - 2) else si.symbol.lastIndexOf('.')
    if (idx >= 0) {
      nativeSymbol(si.symbol.substring(0, idx + 1)).flatMap { outerNativeSym =>
        if (outerNativeSym.allMembersAreVisible) {
          val in = innerName(si)
          Some(NativeSymbol.Inner(in, outerNativeSym, isSubTypeOfJsAny(si)))
        } else {
          None
        }
      }
    } else {
      None
    }
  }

  private def isSubTypeOfJsAny(si: SymbolInformation): Boolean = {
    si.isSubtypeOf("scala/scalajs/js/Any#", symTab)
  }

  private def innerName(si: SymbolInformation): String = {
    val tApi = symbolInformation2TypeSymbolApi(si)
    val as   = tApi.annotations
    checkName(as).orElse(checkExport(as)).getOrElse(tApi.name.toString)
  }

  private def outerNativeClassSymbol(si: SymbolInformation): Option[NativeSymbol.Outer] = {
    val tApi        = symbolInformation2TypeSymbolApi(si)
    val defaultName = tApi.name.toString
    val as          = tApi.annotations
    checkGlobal(si, as, defaultName).orElse(checkImported(si, as)).orElse(checkExported(si))
  }

  private def isAnnotationOfType(t: String)(a: Annotation): Boolean = a.tree.tpe.toString == t

  private def checkName(as: List[Annotation]): Option[String] =
    as.collect {
        case a if isAnnotationOfType("scala.scalajs.js.annotation.JSName")(a) =>
          a.tree.children.tail match {
            case Literal(Constant(value)) :: Nil => Some(String.valueOf(value))
            case _                               => None
          }
      }
      .collect { case Some(s) => s }
      .headOption

  private def checkExport(as: List[Annotation]): Option[String] =
    as.collect {
        case a if isAnnotationOfType("scala.scalajs.js.annotation.JSExport")(a) =>
          a.tree.children.tail match {
            case Literal(Constant(value)) :: Nil => Some(String.valueOf(value))
            case _                               => None
          }
      }
      .collect { case Some(s) => s }
      .headOption

  private def checkGlobal(si: SymbolInformation, as: List[Annotation], defName: String): Option[NativeSymbol.Global] =
    as.collect {
      case a if isAnnotationOfType("scala.scalajs.js.annotation.JSGlobal")(a) =>
        val name = a.tree.children.tail match {
          case Literal(Constant(value)) :: Nil => String.valueOf(value)
          case _                               => defName
        }
        NativeSymbol.Global(name, isSubTypeOfJsAny(si))
    }.headOption

  private def checkImported(si: SymbolInformation, as: List[Annotation]): Option[NativeSymbol.Imported] =
    as.collect {
        case a if isAnnotationOfType("scala.scalajs.js.annotation.JSImport")(a) =>
          a.tree.children.tail match {
            case Literal(Constant(value1)) :: Literal(Constant(value2)) :: _ =>
              Some(NativeSymbol.ImportedName(String.valueOf(value1), String.valueOf(value2), isSubTypeOfJsAny(si)))
            case Literal(Constant(value1)) :: _ =>
              Some(NativeSymbol.ImportedNamespace(String.valueOf(value1), isSubTypeOfJsAny(si)))
            case _ => None
          }
      }
      .collect {
        case Some(s) => s
      }
      .headOption

  private def checkExported(si: SymbolInformation): Option[NativeSymbol.Exported] = topLevelExports.get(si.symbol)

}

object NativeSymbolAnalyzer {

  def apply(topLevelExports: List[TopLevelExport], cl: ClassLoader, symTab: SymbolTable): NativeSymbolAnalyzer = {
    val map = topLevelExports.map {
      case TopLevelExport(name, i: Input.ClsOrObj)      => i.si.symbol -> NativeSymbol.Exported(name, i.allMembersAreVisible)
      case TopLevelExport(name, i: Input.DefOrValOrVar) => i.si.symbol -> NativeSymbol.Exported(name, false)
    }.toMap
    new NativeSymbolAnalyzer(map, cl, symTab)
  }

}

/**
  * Native symbols are symbols that can be accessed in JavaScript.
  *
  * Native symbols are global symbols, imported symbols, or symbols that are nested in other native symbols.
  *
  * (Symbols exported to top level would also count as native symbols. However, they can not be treated here
  * because they are not accessible by reflection: the @JSExportTopLevel annotation is not available during runtime.)
  */
sealed trait NativeSymbol extends Product with Serializable {
  def allMembersAreVisible: Boolean
}

object NativeSymbol {

  /**
    * Native symbols that are directly accessible.
    */
  sealed trait Outer extends NativeSymbol

  sealed trait Imported extends Outer

  case class Global(name: String, allMembersAreVisible: Boolean)                       extends Outer
  case class Exported(name: String, allMembersAreVisible: Boolean)                     extends Outer
  case class ImportedName(module: String, name: String, allMembersAreVisible: Boolean) extends Imported
  case class ImportedNamespace(module: String, allMembersAreVisible: Boolean)          extends Imported
  case class Inner(name: String, outer: NativeSymbol, allMembersAreVisible: Boolean)   extends NativeSymbol

  def moduleName2Id(moduleName: String) = {
    val namePart = moduleName.map {
      case c @ ('$' | '_')                                    => c
      case c if Character.isLetter(c) || Character.isDigit(c) => c
      case _                                                  => '_'
    }
    s"$$$namePart"
  }

  def formatNativeSymbol(n: NativeSymbol): String = n match {
    case NativeSymbol.Global(name, _)                    => name
    case NativeSymbol.ImportedName(module, "default", _) => s"${moduleName2Id(module)}_"
    case NativeSymbol.ImportedName(module, name, _)      => s"${moduleName2Id(module)}.$name"
    case NativeSymbol.ImportedNamespace(module, _)       => s"${moduleName2Id(module)}"
    case NativeSymbol.Inner(name, outer, _)              => s"${formatNativeSymbol(outer)}.$name"
    case NativeSymbol.Exported(name, _)                  => name
  }

}
