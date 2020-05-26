package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.{ClassSignature, TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  def generate(exports: List[Export.TopLevel], symTab: SymbolTable): String = {

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

    def isBuiltInType(symbol: String) = builtInTypeNames.contains(symbol)

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

    def tParam(symbol: String, e: Export): String = {
      val si = e.semSrc.symbolInfo(symbol)
      s"${si.displayName}"
    }

    // breadth first search for a parent class that has been exported
    def findENearestExportedParent(types: Seq[isb.Type]): Option[SimpleName] = {
      if (types.isEmpty) {
        None
      } else {
        val typeSymbols = types.collect {
          case TypeRef(isb.Type.Empty, symbol, tArgs) => symbol
        }
        typeSymbols.find(exportedClasses.contains).map(exportedClasses(_).name).orElse {
          val seq = typeSymbols.map(symTab.info).collect {
            case Some(s) if s.signature.isInstanceOf[ClassSignature] => s.signature.asInstanceOf[ClassSignature].parents
          }.flatten
          findENearestExportedParent(seq)
        }
      }
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

    def memberCtorParam(e: Export.CtorParam): Unit = {
      val returnType = tsType(e.valueSignature.tpe)
      e.mod match {
        case Export.CtorParamMod.Val => sb.append(s"  readonly ${e.name}: $returnType\n")
        case Export.CtorParamMod.Var => sb.append(s"  ${e.name}: $returnType\n")
        case Export.CtorParamMod.Loc =>
      }
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
      val tParams = e.classSignature.typeParameters match {
        case Some(scope) => tsTypes(scope.symlinks.map(tParam(_, e)))
        case None        => ""
      }

      val ext = findENearestExportedParent(e.classSignature.parents).fold("")(p => s" extends $p")

      sb.append(s"export class ${e.name}$tParams$ext {\n")
      val cParams = e.ctorParams
        .map { p =>
          val returnType = tsType(p.valueSignature.tpe)
          s"${p.name}: $returnType"
        }
        .mkString(", ")
      sb.append(s"  constructor($cParams)\n")
      e.ctorParams.foreach {
        case e: Export.CtorParam => memberCtorParam(e)
      }
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

  val builtInTypeNames = Map(
    "java/lang/String#"    -> "string",
    "scala/Boolean#"       -> "boolean",
    "scala/Double#"        -> "number",
    "scala/Int#"           -> "number",
    "scala/Nothing#"        -> "never",
    "scala/Predef.String#" -> "string",
    "scala/Unit#"          -> "void",
  )

  def nonExportedTypeName(symbol: String): String = builtInTypeNames.getOrElse(symbol, opaqueTypeName(symbol))

  def opaqueTypeName(symbol: String) = symbol.substring(0, symbol.length - 1).replace('/', '.')

}
