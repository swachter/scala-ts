package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeArg

import scala.meta.internal.semanticdb.{
  BooleanConstant,
  ByteConstant,
  CharConstant,
  ConstantType,
  DoubleConstant,
  FloatConstant,
  IntConstant,
  LongConstant,
  RepeatedType,
  ShortConstant,
  SingleType,
  StringConstant,
  TypeRef,
  ValueSignature
}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  def generate(exports: List[Input.TopLevel], symTab: SymbolTable): String = {

    val rootNamespace = Namespace.deriveInterfaces(exports, symTab)

    val exportedClassNames = exports.collect {
      case Input.Cls(_, _, Some(name), si, _, _) => si.symbol -> name
    }.toMap

    val exportedObjectNames = exports.collect {
      case Input.Obj(_, _, Some(name), si, _) => si.symbol -> name
    }.toMap

    def exportedTypeName(symbol: Symbol): Option[String] =
      exportedClassNames.get(symbol).orElse(exportedObjectNames.get(symbol)).map(_.str)

    def formatType(tpe: isb.Type): String = tpe match {
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/package.UndefOr#", targs) =>
        s"${formatType(targs(0))} | undefined"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Array#", targs) =>
        s"${formatType(targs(0))}[]"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Dictionary#", targs) =>
        s"{ [key: string]: ${formatType(targs(0))} }"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Promise#", targs) =>
        s"Promise<${formatType(targs(0))}>"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Iterable#", targs) =>
        s"Iterable<${formatType(targs(0))}>"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Iterator#", targs) =>
        s"Iterator<${formatType(targs(0))}>"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Date#", targs) =>
        "Date"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/RegExp#", targs) =>
        "RegExp"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/Symbol#", targs) =>
        "symbol"
      case TypeRef(isb.Type.Empty, symbol, targs) if symbol matches "scala/scalajs/js/Function\\d+#" =>
        val args = targs
          .dropRight(1)
          .zipWithIndex
          .map {
            case (tpe, idx) => s"p${idx + 1}: ${formatType(tpe)}"
          }
          .mkString("(", ", ", ")")
        val returnType = formatType(targs.last)
        s"$args => $returnType"
      case TypeRef(isb.Type.Empty, symbol, targs) if symbol matches "scala/scalajs/js/ThisFunction\\d+#" =>
        val args = targs
          .dropRight(1)
          .zipWithIndex
          .map {
            case (tpe, 0)   => s"this: ${formatType(tpe)}"
            case (tpe, idx) => s"p$idx: ${formatType(tpe)}"
          }
          .mkString("(", ", ", ")")
        val returnType = formatType(targs.last)
        s"$args => $returnType"
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/`|`#", targs) =>
        s"${formatType(targs(0))} | ${formatType(targs(1))}"
      case TypeRef(isb.Type.Empty, symbol, targs) if symbol matches "scala/scalajs/js/Tuple\\d+#" =>
        targs.map(formatType).mkString("[", ", ", "]")
      case TypeRef(isb.Type.Empty, symbol, tArgs) =>
        def tas = formatTypes(tArgs.map(formatType))
        symTab.typeParamSymInfo(symbol) match {
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
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/package.UndefOr#", targs) => s"$name?: ${formatType(targs(0))}"
      case RepeatedType(tpe)                                                   => s"...$name: ${formatType(tpe)}[]"
      case _                                                                   => s"$name: ${formatType(tpe)}"
    }

    def formatMethodParam(symbol: String, e: Input.Def): String = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      formatNameAndType(SimpleName(si.displayName), vs.tpe)
    }

    def tParam(symbol: String, e: Input): String = {
      val si = e.semSrc.symbolInfo(symbol)
      s"${si.displayName}"
    }

    val sb = new StringBuilder

    def exportDef(i: Input.Def): Unit = {
      val tParams = i.methodSignature.typeParameters match {
        case Some(scope) => formatTypes(scope.symlinks.map(tParam(_, i)))
        case None        => ""
      }
      val params     = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i))).mkString(", ")
      val returnType = formatType(i.methodSignature.returnType)
      sb.append(s"export function ${i.name}$tParams($params): $returnType\n")
    }

    def exportVal(i: Input.Val): Unit = {
      val returnType = formatType(i.methodSignature.returnType)
      sb.append(s"export const ${i.name}: $returnType\n")
    }

    def exportVar(i: Input.Var): Unit = {
      val returnType = formatType(i.methodSignature.returnType)
      sb.append(s"export let ${i.name}: $returnType\n")
    }

    def memberDef(i: Input.Def): String = {
      val returnType = formatType(i.methodSignature.returnType)
      if (i.methodSignature.parameterLists.isEmpty) {
        // no parameter lists -> it's a getter
        s"  get ${i.name}(): $returnType\n"
      } else {
        val strings = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i)))
        val params  = strings.mkString(", ")
        if (i.name.str.endsWith("_=")) {
          // name ends with _= -> it's a setter
          s"  set ${i.name.str.dropRight(2)}($params)\n"
        } else {
          s"  ${i.name}($params): $returnType\n"
        }
      }
    }

    def memberVal(i: Input.Val): String = {
      s"  readonly ${formatNameAndType(i.name, i.methodSignature.returnType)}\n"
    }

    def memberVar(i: Input.Var): String = {
      s"  ${formatNameAndType(i.name, i.methodSignature.returnType)}\n"
    }

    def memberCtorParam(i: Input.CtorParam): String = {
      def member = formatNameAndType(i.name, i.valueSignature.tpe)
      i.mod match {
        case Input.CtorParamMod.Val => s"  readonly $member\n"
        case Input.CtorParamMod.Var => s"  $member\n"
        case Input.CtorParamMod.Loc => ""
      }
    }

    def exportObj(i: Input.Obj): Unit = {
      // exportObj is called only if the name is defined, i.e. it is a top level export
      val name = i.name.get
      // export an interface with the same name as the object constant that includes the object members
      // -> this allows the object to be part of a union type
      exportItf(Interface(i.si, name.toFullName, i.member, symTab), 0)
      // -> this allows the object to be part of a union type
      sb.append(s"export const $name: $name\n")
    }

    def isParentTypeKnown(p: ParentType) = rootNamespace.containsItfOrType(p.fullName)

    def exportCls(i: Input.Cls): Unit = {
      // exportCls is called only if the name is defined, i.e. it is a top level export
      val name = i.name.get

      // export an interface with the same name as the exported class if the interface would extends some parent interfaces
      // -> the declaration of that interface and the declaration of the class are "merged"; cf. TypeScript declaration merging
      val itf = Interface(i.si, name.toFullName, Nil, symTab)
      if (itf.parents.exists(isParentTypeKnown)) {
        exportItf(itf, 0)
      }
      val tParams = formatTypes(i.classSignature.typeParamDisplayNames(symTab))

      // check if there is an exported parent class
      val ext = i.si
        .parents(symTab)
        .find(p => p.typeSymbol.filter(exportedClassNames.contains(_)).map(symTab.isClass(_)).getOrElse(false))
        .fold("")(p => s" extends ${formatType(p)}")

      // an interface with the same name is included in the root namespace
      // -> that interface possibly extends base interfaces
      // -> the declaration of the interface and the declaration of the class are "merged"
      //    (cf. TypeScript declaration merging)
      sb.append(s"export class $name$tParams$ext {\n")

      val cParams = i.ctorParams.map(p => formatNameAndType(p.name, p.valueSignature.tpe)).mkString(", ")
      sb.append(s"  constructor($cParams)\n")

      (i.ctorParams ++ i.member).foreach {
        case e: Input.Def       => sb.append(memberDef(e))
        case e: Input.Val       => sb.append(memberVal(e))
        case e: Input.Var       => sb.append(memberVar(e))
        case e: Input.CtorParam => sb.append(memberCtorParam(e))
      }
      sb.append("}\n")
    }

    def exportItf(itf: Interface, indent: Int): Unit = {

      val parents = itf.parents.filter(isParentTypeKnown)

      val ext =
        if (parents.isEmpty) ""
        else parents.map(p => s"${p.fullName}${formatTypes(p.typeArgs.map(formatType))}").mkString(" extends ", ", ", "")

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      sb.append(s"$space${exp}interface ${itf.simpleName}${formatTypes(itf.typeParams)}$ext {\n")

      itf.members
        .map {
          case i: Input.Def       => memberDef(i)
          case i: Input.Val       => memberVal(i)
          case i: Input.Var       => memberVar(i)
          case i: Input.CtorParam => memberCtorParam(i)
        }
        .foreach(m => sb.append(s"$space$m"))

      sb.append(s"$space  '${itf.fullName}': never\n")
      sb.append(s"$space}\n")
    }

    def exportUnion(union: Union, indent: Int): Unit = {
      val allTypeArgs               = union.members.flatMap(_.typeArgs)
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

    def exportAlias(tpe: Alias, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      sb.append(s"$space${exp}type ${tpe.simpleName}${formatTypes(tpe.typeParamDisplayNames(symTab))} = ${formatType(tpe.rhs)}\n")
    }

    def exportNs(ns: Namespace, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      if (indent >= 0) {
        sb.append(s"$space${exp}namespace ${ns.name} {\n")
      }
      ns.itfs.values.foreach(exportItf(_, indent + 2))
      ns.unions.values.foreach(exportUnion(_, indent + 2))
      ns.aliases.values.foreach(exportAlias(_, indent + 2))
      ns.nested.values.foreach(exportNs(_, indent + 2))
      if (indent >= 0) {
        sb.append(s"$space}\n")
      }
    }

    exports.foreach {
      case i: Input.Def                     => exportDef(i)
      case i: Input.Val                     => exportVal(i)
      case i: Input.Var                     => exportVar(i)
      case i: Input.Obj if i.name.isDefined => exportObj(i)
      case i: Input.Cls if i.name.isDefined => exportCls(i)
      case _: Input.Trait | _: Input.Alias  => () // traits and type aliases are already processed and included in their namespace
    }

    val unions = Union.unions(exports)
    unions.foreach(rootNamespace += _)

    exportNs(rootNamespace, -2)

    sb.toString
  }

  def nonExportedTypeName(symbol: String): String = BuiltIn.builtInTypeNames.getOrElse(symbol, FullName(symbol).str)

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
