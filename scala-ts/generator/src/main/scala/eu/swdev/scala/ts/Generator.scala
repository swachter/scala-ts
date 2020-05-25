package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.{TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  def generate(exports: List[Export], symTab: SymbolTable): String = {

    val exportedClasses = exports.collect {
      case e: Export.Cls => e.si.symbol -> e
    }.toMap

    def tsType(tpe: isb.Type): String = tpe match {
      case TypeRef(isb.Type.Empty, symbol, tArgs) =>
        def tas = tsTypes(tArgs.map(tsType))
        symTab.typeParameter(symbol) match {
          case Some(si) => si.displayName
          case None =>
            exportedClasses.get(symbol) match {
              case Some(e) => s"${e.name.str}$tas"
              case None    => s"${nonExportedTypeName(symbol)}$tas"
            }
        }

      case _ => "any"
    }

    def tsTypes(args: Seq[String]): String = if (args.isEmpty) "" else args.mkString("<", ",", ">")

    def isBuiltInType(symbol: String) = builtInTypeName(symbol).isDefined

    def isTypeParameter(symbol: String) = symTab.info(symbol).fold(false)(_.kind == Kind.TYPE_PARAMETER)

    def isOpaqueType(tpe: isb.Type): Boolean = tpe match {
      case TypeRef(isb.Type.Empty, symbol, args) =>
        exportedClasses.get(symbol) match {
          case Some(_) => false
          case None    => !isBuiltInType(symbol) && !isTypeParameter(symbol)
        }
      case _ => false
    }

    def param(symbol: String, e: Export.Def): String = {
      val si  = e.semSrc.symbolInfo(symbol)
      val vs  = si.signature.asInstanceOf[ValueSignature]
      val tpe = tsType(vs.tpe)
      s"${si.displayName}: $tpe"
    }

    def tParam(symbol: String, e: Export.Def): String = {
      val si = e.semSrc.symbolInfo(symbol)
      s"${si.displayName}"
    }

    val sb = new StringBuilder

    def exportDef(e: Export.Def): Unit = {
      val tParams = e.methodSignature.typeParameters match {
        case Some(scope) => tsTypes(scope.symlinks.map(tParam(_, e)))
        case None        => ""
      }
      val params     = e.methodSignature.parameterLists.flatMap(_.symlinks.map(param(_, e))).mkString(", ")
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"export function ${e.name}$tParams($params): $returnType\n")
    }

    def exportVal(e: Export.Val): Unit = {
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"export const ${e.name}: $returnType\n")
    }

    def exportVar(e: Export.Var): Unit = {
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"export let ${e.name}: $returnType\n")
    }

    def memberDef(e: Export.Def): Unit = {
      val params     = e.methodSignature.parameterLists.flatMap(_.symlinks.map(param(_, e))).mkString(", ")
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"  ${e.name}($params): $returnType\n")
    }

    def memberVal(e: Export.Val): Unit = {
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"  readonly ${e.name}: $returnType\n")
    }

    def memberVar(e: Export.Var): Unit = {
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"  ${e.name}: $returnType\n")
    }

    def exportObj(e: Export.Obj): Unit = {
      sb.append(s"export const ${e.name}: {\n")
      e.member.foreach {
        case e: Export.Def => memberDef(e)
        case e: Export.Val => memberVal(e)
        case e: Export.Var => memberVar(e)
      }
      sb.append("}\n")
    }

    def exportCls(e: Export.Cls): Unit = {
      sb.append(s"export class ${e.name} {\n")
      e.member.foreach {
        case e: Export.Def => memberDef(e)
        case e: Export.Val => memberVal(e)
        case e: Export.Var => memberVar(e)
      }
      sb.append("}\n")
    }

    def exportItf(itf: Namespace.Interface, indent: Int): Unit = {
      val ext =
        if (itf.parents.isEmpty) "" else itf.parents.map(p => s"${p.name}${tsTypes(p.typeArgs)}").mkString(" extends ", ", ", "")

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      sb.append(s"$space${exp}interface ${itf.name}${tsTypes(itf.typeParams)}$ext {\n")
      sb.append(s"$space  '${itf.fullName}': never\n")
      sb.append(s"$space}\n")
    }

    def exportNs(ns: Namespace, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      sb.append(s"$space${exp}namespace ${ns.name} {\n")
      ns.itfs.values.foreach(exportItf(_, indent + 2))
      ns.nested.values.foreach(exportNs(_, indent + 2))
      sb.append(s"$space}\n")
    }

    exports.foreach {
      case e: Export.Def => exportDef(e)
      case e: Export.Val => exportVal(e)
      case e: Export.Var => exportVar(e)
      case e: Export.Obj => exportObj(e)
      case e: Export.Cls => exportCls(e)
    }

    val opaqueTypes = exports.flatMap(Analyzer.referencedTypes).filter(isOpaqueType)

    val ns = Namespace(opaqueTypes, symTab)

    ns.itfs.values.foreach(exportItf(_, 0))
    ns.nested.values.foreach(exportNs(_, 0))

    sb.toString
  }

  def builtInTypeName(symbol: String): Option[String] = symbol match {
    case "java/lang/String#" => Some("string")
    case "scala/Unit#"       => Some("void")
    case "scala/Int#"        => Some("number")
    case "scala/Double#"     => Some("number")
    case "scala/Boolean#"    => Some("boolean")
    case _                   => None
  }

  def nonExportedTypeName(symbol: String): String = builtInTypeName(symbol).getOrElse(opaqueTypeName(symbol))

  def opaqueTypeName(symbol: String) = symbol.substring(0, symbol.length - 1).replace('/', '.')

}
