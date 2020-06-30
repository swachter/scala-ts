package eu.swdev.scala.ts

import scala.reflect.runtime.{universe => ru}
import ru._
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.reflect.NameTransformer

/**
  * Analyzes if symbols can be accessed from JavaScript.
  *
  * Annotation values are not stored in semantic db. Therefore scala-reflect is used to access these.
  */
class NativeSymbolAnalyzer(cl: ClassLoader, symTab: SymbolTable) {

  val mirror = ru.runtimeMirror(cl)

  def nativeSymbol(sym: String): Option[NativeSymbol] = {
    symTab.typeSymInfo(sym).flatMap { si =>
      si.signature match {
        case TypeSignature(_, _, upperBound) => upperBound.typeSymbol.flatMap(s => nativeSymbol(s))
        case ClassSignature(_, _, _, _)      => nativeSymbol(si)
        case _                               => None
      }
    }
  }

  private def nativeSymbol(si: SymbolInformation): Option[NativeSymbol] = {
    outerNativeSymbol(si).orElse(innerNativeSymbol(si))
  }

  private def symbolInformation2TypeSymbolApi(si: SymbolInformation): TypeSymbolApi = {
    val fn = FullName(si)
    // translate ScalaMeta symbol into corresponding scala-reflect fullName
    val translated = fn.str.replace("`", "").split('.').map(NameTransformer.encode(_)).mkString(".")
    if (translated.endsWith("$")) {
      mirror.staticModule(translated.dropRight(1)).moduleClass.asClass
    } else {
      mirror.staticClass(translated)
    }
  }

  private def innerNativeSymbol(si: SymbolInformation): Option[NativeSymbol.Inner] = {
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
    val t  = symbolInformation2TypeSymbolApi(si)
    val as = t.annotations
    checkName(as).orElse(checkExport(as)).getOrElse(t.name.toString)
  }

  private def outerNativeSymbol(si: SymbolInformation): Option[NativeSymbol.Outer] = {
    val tApi        = symbolInformation2TypeSymbolApi(si)
    val defaultName = tApi.name.toString
    val as          = tApi.annotations
    checkGlobal(si, as, defaultName).orElse(checkImport(si, as))
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

  private def checkImport(si: SymbolInformation, as: List[Annotation]): Option[NativeSymbol.Imported] =
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

  sealed trait Outer    extends NativeSymbol
  sealed trait Imported extends Outer

  case class Global(name: String, allMembersAreVisible: Boolean)                       extends Outer
  case class ImportedName(module: String, name: String, allMembersAreVisible: Boolean) extends Imported
  case class ImportedNamespace(module: String, allMembersAreVisible: Boolean)          extends Imported
  case class Inner(name: String, outer: NativeSymbol, allMembersAreVisible: Boolean)   extends NativeSymbol
}
