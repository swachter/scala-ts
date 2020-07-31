package eu.swdev.scala.ts

import eu.swdev.scala.ts.SealedTraitSubtypeAnalyzer.SubtypeParam

import scala.meta.internal.semanticdb.{RepeatedType, SymbolInformation, TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

object Generator {

  import TypeFormatter._

  def generate(inputs: Inputs, addRootNamespace: Boolean, symTab: SymbolTable, classLoader: ClassLoader): String = {
    val r = new Result.StringBuilderResult
    generate(inputs, addRootNamespace, symTab, classLoader, r)
    r.sb.toString()
  }

  def generate(inputs: Inputs, addRootNamespace: Boolean, symTab: SymbolTable, classLoader: ClassLoader, result: Result): Unit = {

    val topLevelExports = Analyzer.topLevel(inputs)

    val exportedClassNames = topLevelExports.collect {
      case TopLevelExport(name, i: Input.Cls) => i.si.symbol -> name
    }.toMap

    val nativeSymbolAnalyzer = NativeSymbolAnalyzer(topLevelExports, classLoader, symTab)

    val typeFormatter = new TypeFormatter(addRootNamespace, nativeSymbolAnalyzer, symTab)

    import typeFormatter._

    val rootNamespace = Namespace(
      inputs,
      symTab,
      typeFormatter.isKnownOrBuiltIn,
      nativeSymbolAnalyzer
    )

    // maps object symbols to exported static definitions
    val statics = inputs.flattened.collect {
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

    def valueSymbolInfoAnddSignature(symbol: String, i: Input.Def): (SymbolInformation, ValueSignature) = {
      val si = i.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      (si, vs)
    }

    def formatMethodParam(symbol: String, e: Input.Def): String = {
      val (si, vs) = valueSymbolInfoAnddSignature(symbol, e)
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

    def exportDef(name: String, i: Input.Def): Unit = {
      val tps        = formatTParamSyms(i.methodSignature.typeParamSymbols)
      val params     = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i))).mkString(", ")
      val returnType = typeFormatter(i.methodSignature.returnType)
      result.addLine(s"export function $name$tps($params): $returnType")
    }

    def exportVal(name: String, i: Input.Val): Unit = {
      val returnType = typeFormatter(i.methodSignature.returnType)
      result.addLine(s"export const $name: $returnType")
    }

    def exportVar(name: String, i: Input.Var): Unit = {
      val returnType = typeFormatter(i.methodSignature.returnType)
      result.addLine(s"export let $name: $returnType")
    }

    def memberDef(i: Input.Def, memberKind: MemberKind): String = {
      val tps        = formatTParamSyms(i.methodSignature.typeParamSymbols)
      val returnType = typeFormatter(i.methodSignature.returnType)
      val abs        = if (memberKind.isAbstract) "abstract " else ""
      if (i.methodSignature.parameterLists.isEmpty) {
        // no parameter lists -> it's a getter
        memberKind match {
          case MemberKind.IsInInterface => s"${abs}readonly ${memberName(i)}: $returnType"
          case _                        => s"${abs}get ${memberName(i)}(): $returnType"
        }
      } else {
        val strings = i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i)))
        val params  = strings.mkString(", ")
        if (i.si.displayName.endsWith("_=")) {
          // name ends with _= -> it's a setter
          s"${abs}set ${i.si.displayName.dropRight(2)}($params)"
        } else {
          s"${abs}${memberName(i)}$tps($params): $returnType"
        }
      }
    }

    def memberVal(i: Input.Val, memberKind: MemberKind): String = {
      val abs = if (memberKind.isAbstract) "abstract " else ""
      s"${abs}readonly ${formatNameAndType(memberName(i), i.methodSignature.returnType)}"
    }

    def memberVar(i: Input.Var, memberKind: MemberKind): String = {
      val abs = if (memberKind.isAbstract) "abstract " else ""
      s"${abs}${formatNameAndType(memberName(i), i.methodSignature.returnType)}"
    }

    def memberAccessorPair(i: Input.Def, memberKind: MemberKind): String = {
      val abs = if (memberKind.isAbstract) "abstract " else ""
      s"${abs}${formatNameAndType(memberName(i), i.methodSignature.returnType)}"
    }

    def memberCtorParam(i: Input.CtorParam): String = {
      def member(name: String) = formatNameAndType(name, i.valueSignature.tpe)
      i.mod match {
        case Input.CtorParamMod.Val(name) => s"readonly ${member(name)}"
        case Input.CtorParamMod.Var(name) => s"${member(name)}"
        case Input.CtorParamMod.Prv       => ""
      }
    }

    def memberObj(i: Input.Obj): String = {
      s"readonly ${memberName(i)}: $rootPrefix${FullName(i.si)}"
    }

    def exportObj(name: String, i: Input.Obj): Unit = {
      // export an interface with the same name as the object constant that includes the object members
      // -> this allows the object to be part of a union type
      exportItf(Output.Interface(i.si, FullName.fromSimpleName(s"$name$$"), i.member, symTab), "export ")
      result.addLine(s"export const $name: $name$$")
    }

    def isParentTypeKnown(p: ParentType) = rootNamespace.contains(p.fullName)

    def members(inputs: List[Input], memberOf: MemberOf): List[String] = {

      def isGetter(i: Input.Def): Boolean = i.visibility.isMember && i.methodSignature.parameterLists.isEmpty

      def isSetter(i: Input.Def): Boolean =
        i.visibility.isMember && i.si.displayName.endsWith("_=") && i.methodSignature.parameterLists.flatMap(_.symlinks).size == 1

      val getters = inputs.collect {
        case i: Input.Def if isGetter(i) => i.si.displayName -> i
      }.toMap

      val setters = inputs.collect {
        case i: Input.Def if isSetter(i) => i.si.displayName -> i
      }.toMap

      def commonType(getter: Input.Def, setter: Input.Def): Option[isb.Type] = {
        (getter.methodSignature.returnType,
         setter.methodSignature.parameterLists.flatMap(_.symlinks.map(sym => valueSymbolInfoAnddSignature(sym, setter)))) match {
          case (gt, Seq((si, vs))) =>
            (gt.typeSymbol(symTab), vs.tpe.typeSymbol(symTab)) match {
              case (Some(s1), Some(s2)) if s1 == s2 => Some(gt)
              case _                                => None
            }
          case _ => None
        }
      }

      val accessorPairs = getters
        .map {
          case (dn, gettter) => dn -> (gettter, setters.get(s"${dn}_="))
        }
        .collect {
          case (dn, (getter, Some(setter))) => dn -> (getter, setter, commonType(getter, setter))
        }
        .collect {
          case (dn, (getter, setter, Some(tpe))) => dn -> (getter, setter, tpe)
        }

      def memberDefOrAccessorPair(i: Input.Def, memberKind: MemberKind): String = {
        val dn = i.si.displayName
        accessorPairs.get(dn) match {
          case Some((getter, _, _)) => memberAccessorPair(getter, memberKind)
          case None =>
            Option(dn).filter(_.endsWith("_=")).flatMap(n => accessorPairs.get(n.dropRight(2))) match {
              case Some(_) => "" // no output for the setter of an accessor pair
              case None    => memberDef(i, memberKind)
            }
        }
      }

      val x = symTab.info("scala/Predef.String#")
      val y = symTab.info("java/lang/String#")

      inputs
        .collect {
          case i: Input.Def if i.visibility.isMember => memberDefOrAccessorPair(i, memberOf.memberKind(i.isAbstract))
          case i: Input.Val if i.visibility.isMember => memberVal(i, memberOf.memberKind(i.isAbstract))
          case i: Input.Var if i.visibility.isMember => memberVar(i, memberOf.memberKind(i.isAbstract))
          case i: Input.CtorParam if i.isVisible     => memberCtorParam(i)
          case i: Input.Obj if i.isVisibleMember     => memberObj(i)
          case _                                     => ""
        }
        .filter(_.nonEmpty)
    }

    def exportCls(name: String, i: Input.Cls): Unit = {

      // export an interface with the same name as the exported class if the interface would extends some parent interfaces
      // -> the declaration of that interface and the declaration of the class are "merged"; cf. TypeScript declaration merging
      val itf = Output.Interface(i.si, FullName.fromSimpleName(name), Nil, symTab)
      if (itf.parents.exists(isParentTypeKnown)) {
        exportItf(itf, "export ")
      }
      val tps = formatTParamSyms(i.classSignature.typeParamSymbols)

      // check if there is an exported parent class
      val ext = i.si
        .parents(symTab)
        .find(p => p.typeSymbol(symTab).filter(exportedClassNames.contains(_)).map(symTab.isClass(_)).getOrElse(false))
        .fold("")(p => s" extends ${typeFormatter(p)}")

      val abst = if (i.isAbstract) " abstract" else ""

      // an interface with the same name is included in the root namespace
      // -> that interface possibly extends base interfaces
      // -> the declaration of the interface and the declaration of the class are "merged"
      //    (cf. TypeScript declaration merging)
      result.openBlock(s"export$abst class $name$tps$ext")

      val objSymbol = s"${i.si.symbol.dropRight(1)}."
      statics.get(objSymbol).foreach {
        _.map {
          case e: Input.Def => memberDef(e, MemberKind.IsNonAbstract)
          case e: Input.Val => memberVal(e, MemberKind.IsNonAbstract)
          case e: Input.Var => memberVar(e, MemberKind.IsNonAbstract)
        }.foreach(m => result.addLine(s"static $m"))
      }
      val cParams = i.ctorParams.map(p => formatNameAndType(p.name, p.valueSignature.tpe)).mkString(", ")
      result.addLine(s"constructor($cParams)")

      val ms = members(i.ctorParams ++ i.member, MemberOf.Cls)
      ms.foreach(m => result.addLine(m))
      result.closeBlock()
    }

    def exportItf(itf: Output.Interface, exp: String): Unit = {

      val parents = itf.parents.filter(isParentTypeKnown)

      val ext =
        if (parents.isEmpty) ""
        else parents.map(p => s"${p.fullName}${formatTypes(p.typeArgs)}").mkString(" extends ", ", ", "")

      result.openBlock(s"${exp}interface ${itf.simpleName}${formatTParamSyms(itf.typeParamSyms)}$ext")

      members(itf.members, MemberOf.Itf).foreach(m => result.addLine(m))

      result.addLine(s"'${itf.fullName}': never")
      result.closeBlock()
    }

    def exportUnion(union: Output.Union, exp: String): Unit = {
      val allTypeArgs               = union.members.flatMap(_.typeParams(symTab))
      val (parentArgs, privateArgs) = SubtypeParam.removeDuplicatesAndSplit(allTypeArgs)

      val parentTParams = union.sealedTrait.classSignature.typeParamSymbols.map(TParam(_, symTab)).zipWithIndex.map(_.swap).toMap

      val unionTParams = (parentArgs ++ privateArgs).map {
        case SubtypeParam.Parent(idx)            => formatTParam(parentTParams(idx))
        case SubtypeParam.Unrelated(prefix, sym) => s"$$M$prefix${formatTParamSym(sym)}"
      }

      val members = union.members
        .map { member =>
          val tArgs = member.typeParams(symTab).map {
            case SubtypeParam.Parent(idx)            => parentTParams(idx).displayName
            case SubtypeParam.Unrelated(prefix, sym) => s"$$M$prefix${TParam(sym, symTab).displayName}"
          }
          s"${member.name}${formatTypeNames(tArgs)}"
        }
        .mkString(" | ")

      result.addLine(s"${exp}type ${union.fullName.last}${formatTypeNames(unionTParams)} = $members")
    }

    def exportAlias(tpe: Output.Alias, exp: String): Unit = {
      val tps   = formatTParamSyms(tpe.e.si.typeParamSymbols)
      result.addLine(s"${exp}type ${tpe.simpleName}$tps = ${typeFormatter(tpe.rhs)}")
    }

    def exportNs(ns: Namespace, needsExport: Boolean): Unit = {
      val nsOpened = if (ns.name.isEmpty) {
        false
      } else {
        val exp = if (needsExport) "export " else ""
        result.openBlock(s"${exp}namespace ${ns.name}")
        true
      }

//      val nsOpened = if (ns.name.isEmpty && addRootNamespace) {
//        result.openBlock(s"export namespace _root_")
//        true
//      } else if (ns.name.isEmpty) {
//        false
//      } else {
//        val exp = if (needsExport) "export " else ""
//        result.openBlock(s"${exp}namespace ${ns.name}")
//        true
//      }

      val memberExport = if (nsOpened) "" else "export "

      ns.types.values.foreach {
        case i: Output.Alias     => exportAlias(i, memberExport)
        case i: Output.Interface => exportItf(i, memberExport)
        case i: Output.Union     => exportUnion(i, memberExport)
      }

      ns.nested.values.foreach(exportNs(_, !nsOpened))

      if (nsOpened) {
        result.closeBlock()
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
        result.addLine(s"import $str from '$module'")
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

    exportNs(rootNamespace, true)

    // TypeScript does not support a "_root_" namespace indicator comparable to Scala's _root_ package indicator
    // -> generate aliases for the top level namespaces that are used when referencing types
    if (addRootNamespace) {
      rootNamespace.nested.keys.foreach(ns => result.addLine(s"import $rootPrefix$ns = $ns"))
    }
  }

  sealed trait MemberOf {
    def memberKind(inputIsAbstract: Boolean): MemberKind
  }

  object MemberOf {
    object Itf extends MemberOf {
      override def memberKind(inputIsAbstract: Boolean): MemberKind = MemberKind.IsInInterface
    }
    object Cls extends MemberOf {
      override def memberKind(inputIsAbstract: Boolean): MemberKind =
        if (inputIsAbstract) MemberKind.IsAbstract else MemberKind.IsNonAbstract
    }
  }

  sealed trait MemberKind {
    def isAbstract: Boolean
  }

  object MemberKind {
    object IsNonAbstract extends MemberKind {
      override def isAbstract: Boolean = false
    }
    object IsAbstract extends MemberKind {
      override def isAbstract: Boolean = true
    }
    object IsInInterface extends MemberKind {
      // a member must not be marked as abstract in interfaces
      override def isAbstract: Boolean = false
    }
  }

}
