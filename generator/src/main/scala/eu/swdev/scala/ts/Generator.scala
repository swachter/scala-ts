package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeArg

import scala.meta.internal.semanticdb.{RepeatedType, TypeRef, TypeSignature, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  import TypeFormatter._

  def generate(inputs: List[Input.Defn], symTab: SymbolTable, custom: Seq[CTypeFormatter], classLoader: ClassLoader): String = {

    val topLevel = Analyzer.topLevel(inputs)

    val exportedClassNames = topLevel.collect {
      case TopLevelExport(name, i: Input.Cls) => i.si.symbol -> name
    }.toMap

    val exportedObjectNames = topLevel.collect {
      case TopLevelExport(name, i: Input.Obj) => i.si.symbol -> name
    }.toMap

    def exportedTypeName(symbol: Symbol): Option[String] =
      exportedClassNames.get(symbol).orElse(exportedObjectNames.get(symbol))

    val typeFormatter = new TypeFormatter(custom, exportedTypeName, symTab)

    import typeFormatter._

    val nativeAnalyzer = new NativeAnalyzer(classLoader)

    val (rootNamespace, globalOrImported) = Namespace.deriveInterfaces(
      inputs,
      symTab,
      typeFormatter.isKnownOrBuiltIn,
      nativeAnalyzer
    )

    globalOrImported.foreach(typeFormatter.namedNativeTypes += _)

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

    def tParam(symbol: String, e: Input): String = {
      val si = e.semSrc.symbolInfo(symbol)
      val ts = si.signature.asInstanceOf[TypeSignature]
      val ub = ts.upperBound.typeSymbol match {
        case Some("scala/Any#") => ""
        case Some(_) => s" extends ${formatType(ts.upperBound)}"
        case None => ""
      }
      s"${si.displayName}$ub"
    }

    def tParams(symbols: Seq[Symbol], i: Input): String = {
      formatTypeNames(symbols.map(tParam(_, i)))
    }

    val sb = new StringBuilder

    def exportDef(name: String, i: Input.Def): Unit = {
      val tps = tParams(i.methodSignature.typeParamSymbols, i)
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

    def memberDef(i: Input.Def): String = {
      val tps = tParams(i.methodSignature.typeParamSymbols, i)
      val returnType = typeFormatter(i.methodSignature.returnType)
      if (i.methodSignature.parameterLists.isEmpty) {
        // no parameter lists -> it's a getter
        s"  get ${i.memberName}(): $returnType\n"
      } else {
        val strings = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i)))
        val params  = strings.mkString(", ")
        if (i.memberName.endsWith("_=")) {
          // name ends with _= -> it's a setter
          s"  set ${i.memberName.dropRight(2)}($params)\n"
        } else {
          s"  ${i.memberName}$tps($params): $returnType\n"
        }
      }
    }

    def memberVal(i: Input.Val): String = {
      s"  readonly ${formatNameAndType(i.memberName, i.methodSignature.returnType)}\n"
    }

    def memberVar(i: Input.Var): String = {
      s"  ${formatNameAndType(i.memberName, i.methodSignature.returnType)}\n"
    }

    def memberCtorParam(i: Input.CtorParam): String = {
      def member = formatNameAndType(i.name, i.valueSignature.tpe)
      i.mod match {
        case Input.CtorParamMod.Val => s"  readonly $member\n"
        case Input.CtorParamMod.Var => s"  $member\n"
        case Input.CtorParamMod.Prv => ""
      }
    }

    def memberObj(i: Input.Obj): String = {
      s"readonly ${i.memberName}: ${FullName(i.si)}"
    }

    def exportObj(name: String, i: Input.Obj): Unit = {
      // export an interface with the same name as the object constant that includes the object members
      // -> this allows the object to be part of a union type
      exportItf(Output.Interface(i.si, FullName.fromSimpleName(s"$name$$"), i.member, symTab), 0)
      sb.append(s"export const $name: $name$$\n")
    }

    def isParentTypeKnown(p: ParentType) = rootNamespace.contains(p.fullName)

    def exportCls(name: String, i: Input.Cls): Unit = {

      // export an interface with the same name as the exported class if the interface would extends some parent interfaces
      // -> the declaration of that interface and the declaration of the class are "merged"; cf. TypeScript declaration merging
      val itf = Output.Interface(i.si, FullName.fromSimpleName(name), Nil, symTab)
      if (itf.parents.exists(isParentTypeKnown)) {
        exportItf(itf, 0)
      }
      val tps = tParams(i.classSignature.typeParamSymbols, i)

      // check if there is an exported parent class
      val ext = i.si
        .parents(symTab)
        .find(p => p.typeSymbol.filter(exportedClassNames.contains(_)).map(symTab.isClass(_)).getOrElse(false))
        .fold("")(p => s" extends ${typeFormatter(p)}")

      // an interface with the same name is included in the root namespace
      // -> that interface possibly extends base interfaces
      // -> the declaration of the interface and the declaration of the class are "merged"
      //    (cf. TypeScript declaration merging)
      sb.append(s"export class $name$tps$ext {\n")

      val cParams = i.ctorParams.map(p => formatNameAndType(p.name, p.valueSignature.tpe)).mkString(", ")
      sb.append(s"  constructor($cParams)\n")

      (i.ctorParams ++ i.member).foreach {
        case e: Input.Def       => sb.append(memberDef(e))
        case e: Input.Val       => sb.append(memberVal(e))
        case e: Input.Var       => sb.append(memberVar(e))
        case e: Input.CtorParam => sb.append(memberCtorParam(e))
        case e: Input.Type      =>
      }
      sb.append("}\n")
    }

    def exportItf(itf: Output.Interface, indent: Int): Unit = {

      val parents = itf.parents.filter(isParentTypeKnown)

      val ext =
        if (parents.isEmpty) ""
        else parents.map(p => s"${p.fullName}${formatTypes(p.typeArgs)}").mkString(" extends ", ", ", "")

      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent

      sb.append(s"$space${exp}interface ${itf.simpleName}${formatTypeNames(itf.typeParams)}$ext {\n")

      itf.members
        .map {
          case i: Input.Def               => memberDef(i)
          case i: Input.Val               => memberVal(i)
          case i: Input.Var               => memberVar(i)
          case i: Input.CtorParam         => memberCtorParam(i)
          case i: Input.Obj if i.isMember => memberObj(i)
          case i: Input.Type              => ""
        }
        .filter(!_.isEmpty)
        .foreach(m => sb.append(s"$space$m"))

      sb.append(s"$space  '${itf.fullName}': never\n")
      sb.append(s"$space}\n")
    }

    def exportUnion(union: Output.Union, indent: Int): Unit = {
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
          (s"${member.name}${formatTypeNames(tArgs)}" :: accu._1, count)
        })

      val members = subtypes.reverse.mkString(" | ")
      sb.append(s"$space${exp}type ${union.fullName.last}${formatTypeNames(tParams)} = $members\n")
    }

    def exportAlias(tpe: Output.Alias, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      val tps = tParams(tpe.e.si.typeParamSymbols, tpe.e)

      sb.append(s"$space${exp}type ${tpe.simpleName}$tps = ${typeFormatter(tpe.rhs)}\n")
    }

    def exportNs(ns: Namespace, indent: Int): Unit = {
      val exp   = if (indent == 0) "export " else ""
      val space = "  " * indent
      if (indent >= 0) {
        sb.append(s"$space${exp}namespace ${ns.name} {\n")
      }
      ns.types.values.foreach {
        case i: Output.Alias     => exportAlias(i, indent + 2)
        case i: Output.Interface => exportItf(i, indent + 2)
        case i: Output.Union     => exportUnion(i, indent + 2)
      }
      ns.nested.values.foreach(exportNs(_, indent + 2))
      if (indent >= 0) {
        sb.append(s"$space}\n")
      }
    }

    val mods2Names = typeFormatter.namedNativeTypes.toList.collect {
      case (_, Nativeness.Imported(module, name)) => module -> name
    }.groupBy(_._1).mapValues(l => l.map(_._2).toSet)

    mods2Names.toList.sortBy(_._1).foreach { case (module, names) =>
      val defImport = if (names.contains("default")) Some(s"${moduleName2Id(module)}_") else None
      val namespaceExport = if (names.exists(_ != "default")) Some(s"* as ${moduleName2Id(module)}") else None
      val str = (defImport ++ namespaceExport.toSeq).mkString(", ")
      sb.append(s"import $str from '$module'\n")
    }

    topLevel.foreach {
      case TopLevelExport(n, i: Input.Def) => exportDef(n, i)
      case TopLevelExport(n, i: Input.Val) => exportVal(n, i)
      case TopLevelExport(n, i: Input.Var) => exportVar(n, i)
      case TopLevelExport(n, i: Input.Obj) => exportObj(n, i)
      case TopLevelExport(n, i: Input.Cls) => exportCls(n, i)
    }

    val unions = Output.unions(inputs)
    unions.foreach(rootNamespace += _)

    exportNs(rootNamespace, -2)

    sb.toString
  }

}
