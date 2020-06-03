package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{BooleanConstant, ByteConstant, CharConstant, ClassSignature, ConstantType, DoubleConstant, FloatConstant, IntConstant, LongConstant, ShortConstant, SingleType, StringConstant, TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  def generate(exports: List[Export.TopLevel], symTab: SymbolTable): String = {

    val exportedClasses = exports.collect {
      case e: Export.Cls => e.si.symbol -> e
    }.toMap

    val exportedObjects = exports.collect {
      case e: Export.Obj => e.si.symbol -> e
    }.toMap

    val exportedTraits = exports.collect {
      case e: Export.Trt => e.si.symbol -> e
    }.toMap

    def exportedTypeName(symbol: Symbol): Option[String] =
      exportedClasses
        .get(symbol)
        .map(_.name.str)
        .orElse(exportedObjects.get(symbol).map(_.name.str))
        .orElse(exportedTraits.get(symbol).map(_ => fullName(symbol).str))

    def isBuiltInType(symbol: String) = builtInTypeNames.contains(symbol)

    def isOpaqueType(tpe: isb.Type): Boolean = tpe match {
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/package.UndefOr#", _) => false
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Array#", _)           => false
      case TypeRef(isb.Type.Empty, symbol, _) =>
        exportedTypeName(symbol) match {
          case Some(_) => false
          case None    => !isBuiltInType(symbol)
        }
      case SingleType(isb.Type.Empty, symbol) =>
        exportedTypeName(symbol) match {
          case Some(_) => false
          case None    => true
        }
      case _ => false
    }

    val referencedTypes = exports.flatMap(Analyzer.referencedTypes(_, symTab))
    val opaqueTypes     = referencedTypes.filter(isOpaqueType)

    val ns = new Namespace(SimpleName(""))

    def formatType(tpe: isb.Type): String = tpe match {
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/package.UndefOr#", targs) =>
        s"${formatType(targs(0))} | undefined"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Array#", targs) =>
        s"${formatType(targs(0))}[]"
      case TypeRef(isb.Type.Empty, symbol, tArgs) =>
        def tas = formatTypes(tArgs.map(formatType))
        symTab.typeParameter(symbol) match {
          case Some(si) => si.displayName
          case None =>
            exportedTypeName(symbol) match {
              case Some(str) => s"$str$tas"
              case None      => s"${nonExportedTypeName(symbol)}$tas"
            }
        }
      case SingleType(isb.Type.Empty, symbol) =>
        exportedTypeName(symbol) match {
          case Some(str) => str
          case None      => nonExportedTypeName(symbol)
        }

      case ConstantType(constant) =>
        constant match {
          case BooleanConstant(value) => String.valueOf(value)
          case ByteConstant(value)    => String.valueOf(value)
          case CharConstant(value)    => "object" // ScalaJS represents char as object
          case DoubleConstant(value)  => String.valueOf(value)
          case FloatConstant(value)   => String.valueOf(value)
          case IntConstant(value)     => String.valueOf(value)
          case LongConstant(value)    => "object" // ScalaJS represents long as object
          case ShortConstant(value)   => String.valueOf(value)
          case StringConstant(value)  => s"'${escapeString(value)}'"
          case _                      => "any"
        }

      case _ => "any"
    }

    def formatTypes(args: Seq[String]): String = if (args.isEmpty) "" else args.mkString("<", ",", ">")

    def formatNameAndType(name: SimpleName, tpe: isb.Type): String = tpe match {
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/package.UndefOr#", targs) =>
        s"$name?: ${formatType(targs(0))}"
      case _ => s"$name: ${formatType(tpe)}"
    }

    def formatMethodParam(symbol: String, e: Export.Def): String = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      formatNameAndType(SimpleName(si.displayName), vs.tpe)
    }

    def tParam(symbol: String, e: Export): String = {
      val si = e.semSrc.symbolInfo(symbol)
      s"${si.displayName}"
    }

    // breadth first search for a parent class that has been exported
    def findNearestExportedParentClass(types: Seq[isb.Type]): Option[SimpleName] = {
      if (types.isEmpty) {
        None
      } else {
        val typeSymbols = types.collect {
          case TypeRef(isb.Type.Empty, symbol, tArgs) => symbol
        }
        typeSymbols.find(exportedClasses.contains).map(exportedClasses(_).name).orElse {
          val seq = typeSymbols
            .map(symTab.info)
            .collect {
              case Some(s) if s.signature.isInstanceOf[ClassSignature] => s.signature.asInstanceOf[ClassSignature].parents
            }
            .flatten
          findNearestExportedParentClass(seq)
        }
      }
    }

    val sb = new StringBuilder

    def exportDef(e: Export.Def): Unit = {
      val tParams = e.methodSignature.typeParameters match {
        case Some(scope) => formatTypes(scope.symlinks.map(tParam(_, e)))
        case None        => ""
      }
      val params     = e.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, e))).mkString(", ")
      val returnType = formatType(e.methodSignature.returnType)
      sb.append(s"export function ${e.name}$tParams($params): $returnType\n")
    }

    def exportVal(e: Export.Val): Unit = {
      val returnType = formatType(e.methodSignature.returnType)
      sb.append(s"export const ${e.name}: $returnType\n")
    }

    def exportVar(e: Export.Var): Unit = {
      val returnType = formatType(e.methodSignature.returnType)
      sb.append(s"export let ${e.name}: $returnType\n")
    }

    def memberDef(e: Export.Def): String = {
      val returnType = formatType(e.methodSignature.returnType)
      if (e.methodSignature.parameterLists.isEmpty) {
        // no parameter lists -> it's a getter
        s"  get ${e.name}(): $returnType\n"
      } else {
        val strings = e.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, e)))
        val params  = strings.mkString(", ")
        if (e.name.str.endsWith("_=")) {
          // name ends with _= -> it's a setter
          s"  set ${e.name.str.dropRight(2)}($params)\n"
        } else {
          s"  ${e.name}($params): $returnType\n"
        }
      }
    }

    def memberVal(e: Export.Val): String = {
      s"  readonly ${formatNameAndType(e.name, e.methodSignature.returnType)}\n"
    }

    def memberVar(e: Export.Var): String = {
      s"  ${formatNameAndType(e.name, e.methodSignature.returnType)}\n"
    }

    def memberCtorParam(e: Export.CtorParam): String = {
      def member = formatNameAndType(e.name, e.valueSignature.tpe)
      e.mod match {
        case Export.CtorParamMod.Val => s"  readonly $member\n"
        case Export.CtorParamMod.Var => s"  $member\n"
        case Export.CtorParamMod.Loc => ""
      }
    }

    def exportObj(e: Export.Obj): Unit = {
      sb.append(s"export const ${e.name}: {\n")
      e.member.foreach {
        case e: Export.Def => sb.append(memberDef(e))
        case e: Export.Val => sb.append(memberVal(e))
        case e: Export.Var => sb.append(memberVar(e))
      }
      sb.append("}\n")
    }

    def exportCls(e: Export.Cls): Unit = {
      val tParams = formatTypes(e.classSignature.typeParamDisplayNames(symTab))

      val ext = findNearestExportedParentClass(e.classSignature.parents).fold("")(p => s" extends $p")

      sb.append(s"export class ${e.name}$tParams$ext {\n")
      val cParams = e.ctorParams
        .map(p => formatNameAndType(p.name, p.valueSignature.tpe))
        .mkString(", ")
      sb.append(s"  constructor($cParams)\n")
      e.ctorParams.foreach {
        case e: Export.CtorParam => sb.append(memberCtorParam(e))
      }
      e.member.foreach {
        case e: Export.Def => sb.append(memberDef(e))
        case e: Export.Val => sb.append(memberVal(e))
        case e: Export.Var => sb.append(memberVar(e))
      }
      sb.append("}\n")
    }

    def exportItf(itf: Interface, indent: Int): Unit = {

      val parents = itf.parents.filter(p => ns.containsItf(p.fullName))

      val ext =
        if (parents.isEmpty) ""
        else parents.map(p => s"${p.fullName}${formatTypes(p.typeArgs.map(formatType))}").mkString(" extends ", ", ", "")

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      sb.append(s"$space${exp}interface ${itf.simpleName}${formatTypes(itf.typeParams)}$ext {\n")

      itf.members
        .map {
          case e: Export.Def => memberDef(e)
          case e: Export.Val => memberVal(e)
          case e: Export.Var => memberVar(e)
        }
        .foreach(m => sb.append(s"$space$m"))

      sb.append(s"$space  '${itf.fullName}': never\n")
      sb.append(s"$space}\n")
    }

//    def exportUnion(union: Union, indent: Int): Unit = {
//
//      val tParams = union.sealedTrait.classSignature.typeParameters match {
//        case Some(scope) => formatTypes(scope.symlinks.map(tParam(_, union.sealedTrait)))
//        case None        => ""
//      }
//
//      val exp      = if (indent == 0) "export " else ""
//      val space    = "  " * indent
//      val subtypes = union.members.map(m => s"${m.name.str}${formatTypes(m.typeArgs)}").mkString(" | ")
//
//      sb.append(s"$space${exp}type ${union.name}$tParams = $subtypes\n")
//    }

    def exportNs(ns: Namespace, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      if (indent >= 0) {
        sb.append(s"$space${exp}namespace ${ns.name} {\n")
      }
      ns.itfs.values.foreach(exportItf(_, indent + 2))
      //      ns.unions.values.foreach(exportUnion(_, indent + 2))
      ns.nested.values.foreach(exportNs(_, indent + 2))
      if (indent >= 0) {
        sb.append(s"$space}\n")
      }
    }

    exports.foreach {
      case e: Export.Def => exportDef(e)
      case e: Export.Val => exportVal(e)
      case e: Export.Var => exportVar(e)
      case e: Export.Obj => exportObj(e)
      case e: Export.Cls => exportCls(e)
      case e: Export.Trt => ns += Interface(e, symTab)
    }

    val interfacesForOpaqueTypes = Interface.interfaces(opaqueTypes, symTab)
    interfacesForOpaqueTypes.foreach(ns += _)

    Union.unions(exports).foreach(ns += _)

    exportNs(ns, -2)

    sb.toString
  }

  val builtInTypeNames = Map(
    "java/lang/String#"    -> "string",
    "scala/Boolean#"       -> "boolean",
    "scala/Double#"        -> "number",
    "scala/Int#"           -> "number",
    "scala/Nothing#"       -> "never",
    "scala/Predef.String#" -> "string",
    "scala/Unit#"          -> "void",
  )

  def nonExportedTypeName(symbol: String): String = builtInTypeNames.getOrElse(symbol, opaqueTypeName(symbol))

  def opaqueTypeName(symbol: String) = symbol.substring(0, symbol.length - 1).replace('/', '.').replace(".package.", ".")

  def escapeString(str: String): String = {
    str.flatMap {
      case '\b'                      => "\\b"
      case '\f'                      => "\\f"
      case '\n'                      => "\\n"
      case '\r'                      => "\\r"
      case '\t'                      => "\\t"
      case '\u000b'                  => "\\v"
      case '\''                      => "\\'"
      case '\"'                      => "\\\""
      case '\\'                      => "\\"
      case c if c >= ' ' && c <= 126 => c.toString
      case c                         => f"\\u$c%04x"
    }
  }
}
