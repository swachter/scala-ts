package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeParam

import scala.meta.internal.semanticdb.{RepeatedType, TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  import TypeFormatter._

  def generate(inputs: List[Input.Defn], symTab: SymbolTable, custom: Seq[CTypeFormatter], classLoader: ClassLoader): String = {

    val topLevelExports = Analyzer.topLevel(inputs)

    val exportedClassNames = topLevelExports.collect {
      case TopLevelExport(name, i: Input.Cls) => i.si.symbol -> name
    }.toMap

    val nativeSymbolAnalyzer = NativeSymbolAnalyzer(topLevelExports, classLoader, symTab)

    val typeFormatter = new TypeFormatter(custom, nativeSymbolAnalyzer, symTab)

    import typeFormatter._

    val rootNamespace = Namespace(
      inputs,
      symTab,
      typeFormatter.isKnownOrBuiltIn,
      nativeSymbolAnalyzer
    )

    // maps object symbols to exported static definitions
    val statics = inputs.collect {
      case i: Input.Obj =>
        i.si.symbol -> i.member.collect {
          case i: Input.DefOrValOrVar if i.visibility.isStatic => i
        }
    }.toMap

    def memberName(i: Input.Exportable): String = {
      def mn(s: String): String =
        if (s.nonEmpty && Character.isJavaIdentifierStart(s(0)) && s.substring(1).forall(Character.isJavaIdentifierPart)) {
          s
        } else {
          s"['$s']"
        }
      i.visibility match {
        case Visibility.JSExportWithName(n)       => mn(n)
        case Visibility.JSExportStaticWithName(n) => mn(n)
        case Visibility.JsNameWithString(n)       => mn(n)
        case Visibility.JsNameWithSymbol(s) =>
          nativeSymbolAnalyzer.nativeSymbol(s) match {
            case Some(s) => s"[${NativeSymbol.formatNativeSymbol(s)}]"
            case None    => throw new RuntimeException(s"unknown native symbol: $s")
          }
        case _ => i.si.displayName
      }
    }

    def formatNameAndType(name: String, tpe: isb.Type): String = tpe match {
      case TypeRef(isb.Type.Empty, "scala/scalajs/js/package.UndefOr#", targs) => s"$name?: ${typeFormatter(targs(0))}"
      case RepeatedType(tpe)                                                   => s"...$name: ${typeFormatter(tpe)}[]"
      case _                                                                   => s"$name: ${typeFormatter(tpe)}"
    }

    def formatMethodParam(symbol: String, e: Input.Def): String = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      formatNameAndType(si.displayName, vs.tpe)
    }

    def formatTParamSym(sym: String): String = formatTParam(TParam(sym, symTab))

    def formatTParam(tParam: TParam): String = {
      val e = tParam.upperBound match {
        case Some(t) => s" extends ${formatType(t)}"
        case None    => ""
      }
      s"${tParam.displayName}$e"
    }

    def formatTParamSyms(symbols: Seq[Symbol]): String = formatTypeNames(symbols.map(formatTParamSym(_)))

    val sb = new StringBuilder

    def exportDef(name: String, i: Input.Def): Unit = {
      val tps        = formatTParamSyms(i.methodSignature.typeParamSymbols)
      val params     = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i))).mkString(", ")
      val returnType = typeFormatter(i.methodSignature.returnType)
      sb.append(s"export function $name$tps($params): $returnType\n")
    }

    def exportVal(name: String, i: Input.Val): Unit = {
      val returnType = typeFormatter(i.methodSignature.returnType)
      sb.append(s"export const $name: $returnType\n")
    }

    def exportVar(name: String, i: Input.Var): Unit = {
      val returnType = typeFormatter(i.methodSignature.returnType)
      sb.append(s"export let $name: $returnType\n")
    }

    def memberDef(i: Input.Def, isMemberOfAbstractClass: Boolean): String = {
      val tps        = formatTParamSyms(i.methodSignature.typeParamSymbols)
      val returnType = typeFormatter(i.methodSignature.returnType)
      val abs        = if (isMemberOfAbstractClass && i.isAbstract) "abstract " else ""
      if (i.methodSignature.parameterLists.isEmpty) {
        // no parameter lists -> it's a getter
        s"${abs}get ${memberName(i)}(): $returnType\n"
      } else {
        val strings = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i)))
        val params  = strings.mkString(", ")
        if (i.si.displayName.endsWith("_=")) {
          // name ends with _= -> it's a setter
          s"${abs}set ${i.si.displayName.dropRight(2)}($params)\n"
        } else {
          s"${abs}${memberName(i)}$tps($params): $returnType\n"
        }
      }
    }

    def memberVal(i: Input.Val, isMemberOfAbstractClass: Boolean): String = {
      val abs = if (isMemberOfAbstractClass && i.isAbstract) "abstract " else ""
      s"${abs}readonly ${formatNameAndType(memberName(i), i.methodSignature.returnType)}\n"
    }

    def memberVar(i: Input.Var, isMemberOfAbstractClass: Boolean): String = {
      val abs = if (isMemberOfAbstractClass && i.isAbstract) "abstract " else ""
      s"${abs}${formatNameAndType(memberName(i), i.methodSignature.returnType)}\n"
    }

    def memberCtorParam(i: Input.CtorParam): String = {
      def member(name: String) = formatNameAndType(name, i.valueSignature.tpe)
      i.mod match {
        case Input.CtorParamMod.Val(name) => s"readonly ${member(name)}\n"
        case Input.CtorParamMod.Var(name) => s"${member(name)}\n"
        case Input.CtorParamMod.Prv       => ""
      }
    }

    def memberObj(i: Input.Obj): String = {
      s"readonly ${memberName(i)}: ${FullName(i.si)}\n"
    }

    def exportObj(name: String, i: Input.Obj): Unit = {
      // export an interface with the same name as the object constant that includes the object members
      // -> this allows the object to be part of a union type
      exportItf(Output.Interface(i.si, FullName.fromSimpleName(s"$name$$"), i.member, symTab), 0)
      sb.append(s"export const $name: $name$$\n")
    }

    def isParentTypeKnown(p: ParentType) = rootNamespace.contains(p.fullName)

    def members(inputs: List[Input], canBeAbstract: Boolean): List[String] = {
      inputs
        .collect {
          case i: Input.Def if i.visibility.isMember => memberDef(i, canBeAbstract && i.isAbstract)
          case i: Input.Val if i.visibility.isMember => memberVal(i, canBeAbstract && i.isAbstract)
          case i: Input.Var if i.visibility.isMember => memberVar(i, canBeAbstract && i.isAbstract)
          case i: Input.CtorParam                    => memberCtorParam(i)
          case i: Input.Obj if i.isVisibleMember     => memberObj(i)
        }
        .filter(_.nonEmpty)
    }

    def exportCls(name: String, i: Input.Cls): Unit = {

      // export an interface with the same name as the exported class if the interface would extends some parent interfaces
      // -> the declaration of that interface and the declaration of the class are "merged"; cf. TypeScript declaration merging
      val itf = Output.Interface(i.si, FullName.fromSimpleName(name), Nil, symTab)
      if (itf.parents.exists(isParentTypeKnown)) {
        exportItf(itf, 0)
      }
      val tps = formatTParamSyms(i.classSignature.typeParamSymbols)

      // check if there is an exported parent class
      val ext = i.si
        .parents(symTab)
        .find(p => p.typeSymbol.filter(exportedClassNames.contains(_)).map(symTab.isClass(_)).getOrElse(false))
        .fold("")(p => s" extends ${typeFormatter(p)}")

      val abst = if (i.isAbstract) " abstract" else ""

      // an interface with the same name is included in the root namespace
      // -> that interface possibly extends base interfaces
      // -> the declaration of the interface and the declaration of the class are "merged"
      //    (cf. TypeScript declaration merging)
      sb.append(s"export$abst class $name$tps$ext {\n")

      val objSymbol = s"${i.si.symbol.dropRight(1)}."
      statics.get(objSymbol).foreach {
        _.map {
          case e: Input.Def => memberDef(e, false)
          case e: Input.Val => memberVal(e, false)
          case e: Input.Var => memberVar(e, false)
        }.foreach(m => sb.append(s"  static $m"))
      }
      val cParams = i.ctorParams.map(p => formatNameAndType(p.name, p.valueSignature.tpe)).mkString(", ")
      sb.append(s"  constructor($cParams)\n")

      val ms = members(i.ctorParams ++ i.member, true)
      ms.foreach(m => sb.append(s"  $m"))
      sb.append("}\n")
    }

    def exportItf(itf: Output.Interface, indent: Int): Unit = {

      val parents = itf.parents.filter(isParentTypeKnown)

      val ext =
        if (parents.isEmpty) ""
        else parents.map(p => s"${p.fullName}${formatTypes(p.typeArgs)}").mkString(" extends ", ", ", "")

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      sb.append(s"$space${exp}interface ${itf.simpleName}${formatTParamSyms(itf.typeParamSyms)}$ext {\n")

      members(itf.members, false).foreach(m => sb.append(s"$space  $m"))

      sb.append(s"$space  '${itf.fullName}': never\n")
      sb.append(s"$space}\n")
    }

    def exportUnion(union: Output.Union, indent: Int): Unit = {
      val allTypeArgs               = union.members.flatMap(_.typeParams)
      val (parentArgs, privateArgs) = SubtypeParam.removeDuplicatesAndSplit(allTypeArgs)

      val parentTParams = union.sealedTrait.classSignature.typeParamSymbols.map(TParam(_, symTab)).zipWithIndex.map(_.swap).toMap

      val unionTParams = (parentArgs ++ privateArgs).map {
        case SubtypeParam.Parent(idx)            => formatTParam(parentTParams(idx))
        case SubtypeParam.Unrelated(prefix, sym) => s"$$M$prefix${formatTParamSym(sym)}"
      }

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      val members = union.members
        .map { member =>
          val tArgs = member.typeParams.map {
            case SubtypeParam.Parent(idx)            => parentTParams(idx).displayName
            case SubtypeParam.Unrelated(prefix, sym) => s"$$M$prefix${TParam(sym, symTab).displayName}"
          }
          s"${member.name}${formatTypeNames(tArgs)}"
        }
        .mkString(" | ")

      sb.append(s"$space${exp}type ${union.fullName.last}${formatTypeNames(unionTParams)} = $members\n")
    }

    def exportAlias(tpe: Output.Alias, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      val tps   = formatTParamSyms(tpe.e.si.typeParamSymbols)

      sb.append(s"$space${exp}type ${tpe.simpleName}$tps = ${typeFormatter(tpe.rhs)}\n")
    }

    def exportNs(ns: Namespace, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      if (indent >= 0) {
        sb.append(s"$space${exp}namespace ${ns.name} {\n")
      }
      ns.types.values.foreach {
        case i: Output.Alias     => exportAlias(i, indent + 1)
        case i: Output.Interface => exportItf(i, indent + 1)
        case i: Output.Union     => exportUnion(i, indent + 1)
      }
      ns.nested.values.foreach(exportNs(_, indent + 1))
      if (indent >= 0) {
        sb.append(s"$space}\n")
      }
    }

    val mods2Names = nativeSymbolAnalyzer.nativeSymbolImports
      .groupBy(_._1)
      .mapValues(l => l.map(_._2).toSet)

    mods2Names.toList.sortBy(_._1).foreach {
      case (module, names) =>
        val defImport       = if (names.contains("default")) Some(s"${NativeSymbol.moduleName2Id(module)}_") else None
        val namespaceExport = if (names.exists(_ != "default")) Some(s"* as ${NativeSymbol.moduleName2Id(module)}") else None
        val str             = (defImport ++ namespaceExport.toSeq).mkString(", ")
        sb.append(s"import $str from '$module'\n")
    }

    topLevelExports.foreach {
      case TopLevelExport(n, i: Input.Def) => exportDef(n, i)
      case TopLevelExport(n, i: Input.Val) => exportVal(n, i)
      case TopLevelExport(n, i: Input.Var) => exportVar(n, i)
      case TopLevelExport(n, i: Input.Obj) => exportObj(n, i)
      case TopLevelExport(n, i: Input.Cls) => exportCls(n, i)
    }

    val (unions, missingInterfaces) = Output.unions(inputs, rootNamespace, symTab)
    unions.foreach(rootNamespace += _)
    missingInterfaces.foreach(rootNamespace += _)

    exportNs(rootNamespace, -1)

    sb.toString
  }

}
