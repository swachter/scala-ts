package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{TypeRef, ValueSignature}
import scala.meta.internal.{semanticdb => isb}

object Generator {

  def generate(exports: List[Export]): String = {

    val exportedClasses = exports.collect {
      case e: Export.Cls => e.si.symbol -> e
    }.toMap

    def tsType(tpe: isb.Type): String = tpe match {
      case TypeRef(isb.Type.Empty, symbol, args) =>
        def tparams = if (args.isEmpty) "" else args.map(tsType).mkString("<", ",", ">")
        exportedClasses.get(symbol) match {
          case Some(e) => s"${e.name.str}$tparams"
          case None => s"${nonExportedTypeName(symbol)}$tparams"
        }
      case _ => "any"
    }

    def isOpaqueType(tpe: isb.Type): Boolean = tpe match {
      case TypeRef(isb.Type.Empty, symbol, args) =>
        exportedClasses.get(symbol) match {
          case Some(_) => false
          case None => builtInTypeName(symbol).isEmpty
        }
      case _ => false
    }

    def arg(symbol: String, e: Export.Def): String = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      val tpe = tsType(vs.tpe)
      s"${si.displayName}: $tpe"
    }

    val sb = new StringBuilder

    def exportDef(e: Export.Def): Unit = {
      val args = e.methodSignature.parameterLists.flatMap(_.symlinks.map(arg(_, e))).mkString(", ")
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"export function ${e.name}($args): $returnType\n")
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
      val args = e.methodSignature.parameterLists.flatMap(_.symlinks.map(arg(_, e))).mkString(", ")
      val returnType = tsType(e.methodSignature.returnType)
      sb.append(s"  ${e.name}($args): $returnType\n")
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

    exports.foreach {
      case e: Export.Def => exportDef(e)
      case e: Export.Val => exportVal(e)
      case e: Export.Var => exportVar(e)
      case e: Export.Obj => exportObj(e)
      case e: Export.Cls => exportCls(e)
    }

    def referencedTypes(e: Export): List[isb.Type] = e match {
      case e: Export.Def => e.methodSignature.returnType :: parameterTypes(e)
      case e: Export.Val => List(e.methodSignature.returnType)
      case e: Export.Var => List(e.methodSignature.returnType)
      case e: Export.Cls => e.member.flatMap(referencedTypes)
      case e: Export.Obj => e.member.flatMap(referencedTypes)
    }

    val opaqueTypes = exports.flatMap(referencedTypes).filter(isOpaqueType)

    sb.toString
  }

  def parameterTypes(e: Export.Def): List[isb.Type] = {
    def argType(symbol: String): isb.Type = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      vs.tpe
    }
    e.methodSignature.parameterLists.flatMap(_.symlinks).map(argType).toList
  }

  def builtInTypeName(symbol: String): Option[String] = symbol match {
    case "java/lang/String#" => Some("string")
    case "scala/Int#" => Some("number")
    case "scala/Double#" => Some("number")
    case "scala/Boolean#" => Some("boolean")
    case _ => None
  }

  def nonExportedTypeName(symbol: String): String = builtInTypeName(symbol).getOrElse(opaqueTypeName(symbol))

  def opaqueTypeName(symbol: String) = symbol.substring(0, symbol.length - 1).replace('/', '.')

}
