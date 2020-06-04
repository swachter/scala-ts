package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeArg
import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeArg.{Parent, Private}

import scala.meta.internal.semanticdb.{BooleanConstant, ByteConstant, CharConstant, ClassSignature, ConstantType, DoubleConstant, FloatConstant, IntConstant, LongConstant, ShortConstant, SingleType, StringConstant, SymbolInformation, TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  def generate(exports: List[Export.TopLevel], symTab: SymbolTable): String = {

    val rootNamespace = Namespace.deriveInterfaces(exports, symTab)

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
              case Some(s) => s.parents
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

    // export interfaces for exported classes / objects if the interface would extends some parent interfaces
    // (the declaration of the exported interfaces are "merged" with the declaration of classes / objects with the
    //  same name; cf. TypeScript declaration merging).
    def exportMergedItf(si: SymbolInformation, name: SimpleName): Unit = {
      val itf = Interface(si, name, Nil, symTab)
      if (itf.parents.exists(p => rootNamespace.containsItf(p.fullName))) {
        exportItf(itf, 0)
      }
    }

    def exportObj(e: Export.Obj): Unit = {
      exportMergedItf(e.si, e.name)

      sb.append(s"export const ${e.name}: {\n")
      e.member.foreach {
        case e: Export.Def => sb.append(memberDef(e))
        case e: Export.Val => sb.append(memberVal(e))
        case e: Export.Var => sb.append(memberVar(e))
      }
      sb.append("}\n")
    }

    def exportCls(e: Export.Cls): Unit = {
      exportMergedItf(e.si, e.name)

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

      val parents = itf.parents.filter(p => rootNamespace.containsItf(p.fullName))

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

    def exportUnion(union: Union, indent: Int): Unit = {
      val allTypeArgs = union.members.flatMap(_.typeArgs)
      val (parentArgs, privateArgs) = SubtypeArg.split(allTypeArgs)

      val parentArgNames = union.typeParamDisplayNames(symTab).zipWithIndex.map(_.swap).toMap

      val (tParams, _) = (parentArgs ++ privateArgs).foldLeft((List.empty[String], 0))((accu, subtypeArg) =>
        subtypeArg match {
          case SubtypeArg.Parent(idx) => (parentArgNames(idx) :: accu._1, accu._2)
          case SubtypeArg.Private     => (s"T${accu._2}$$" :: accu._1, accu._2 + 1)
      })

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      val (subtypes, _) = union.members
        .foldLeft((List.empty[String], 0))((accu, member) => {
          val (tArgs, count) = member.typeArgs.foldLeft((List.empty[String], accu._2))((accu, subtypeArg) =>
            subtypeArg match {
              case SubtypeArg.Parent(idx) => (parentArgNames(idx) :: accu._1, accu._2)
              case SubtypeArg.Private     => (s"T${accu._2}$$" :: accu._1, accu._2 + 1)
          })
          (s"${member.name}${formatTypes(tArgs)}" :: accu._1, count)
        })

      val members = subtypes.reverse.mkString(" | ")
      sb.append(s"$space${exp}type ${union.fullName.last}${formatTypes(tParams)} = $members\n")
    }

    def exportNs(ns: Namespace, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      if (indent >= 0) {
        sb.append(s"$space${exp}namespace ${ns.name} {\n")
      }
      ns.itfs.values.foreach(exportItf(_, indent + 2))
      ns.unions.values.foreach(exportUnion(_, indent + 2))
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
      case e: Export.Trt => ()
    }

    val unions = Union.unions(exports)
    unions.foreach(rootNamespace += _)

    exportNs(rootNamespace, -2)

    sb.toString
  }

  def nonExportedTypeName(symbol: String): String = BuiltIn.builtInTypeNames.getOrElse(symbol, opaqueTypeName(symbol))

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
