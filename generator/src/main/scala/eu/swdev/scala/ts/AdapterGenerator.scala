package eu.swdev.scala.ts

import eu.swdev.scala.ts
import eu.swdev.scala.ts.Input.CtorParam

import scala.meta.internal.semanticdb.{ValueSignature, Type => SdbType}
import scala.meta.internal.symtab.SymbolTable

object AdapterGenerator {

  def generate(inputs: Inputs, symTab: SymbolTable, adapterName: String): String = {
    val r = new ts.Result.StringBuilderResult
    generate(inputs, symTab, adapterName, r)
    r.sb.toString()
  }

  def generate(inputs: Inputs, symTab: SymbolTable, adapterName: String, result: Result): Unit = {

    val root = Adapter(inputs, adapterName)

    val unchangedTypeFormatter = new UnchangedTypeFormatter(symTab)
    val interopTypeFormatter   = new InteropTypeFormatter(symTab)

    def formatTParamSyms(symbols: Seq[Symbol]): String = if (symbols.isEmpty) "" else symbols.map(formatTParamSym).mkString("[", ",", "]")

    def formatTParamSym(sym: String): String = formatTParam(TParam(sym, symTab))

    def formatTParam(tParam: TParam): String = {
      val lb = tParam.lowerBound match {
        case Some(t) => s" >: ${unchangedTypeFormatter(t)}"
        case None    => ""
      }
      val ub = tParam.upperBound match {
        case Some(t) => s" <: ${unchangedTypeFormatter(t)}"
        case None    => ""
      }
      s"${tParam.displayName}$lb$ub"
    }

    def formatMethodParam(symbol: String, i: Input.Def): String = {
      val si = i.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      s"${si.displayName}: ${interopTypeFormatter(vs.tpe)}"
    }

    def formatMethodArg(symbol: String, i: Input.Def): String = {
      val si = i.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      val conv = if (interopTypeFormatter.needsConverter(vs.tpe)) {
        s".$$cnv[${unchangedTypeFormatter(vs.tpe)}]"
      } else {
        ""
      }
      s"${si.displayName}$conv"
    }

    def displayNameAndAccessName(input: Input, tracker: PathTracker): (String, String) = {
      val displayName = input.si.displayName
      val accessName  = s"${tracker.delegatePath}.$displayName"
      (displayName, accessName)
    }

    def resOrCnv(interopType: Option[String]) = interopType.fold(".$res")(s => s".$$cnv[$s]")

    def outputDef(input: Input.Def, tracker: PathTracker): Unit = {
      val (displayName, accessName) = displayNameAndAccessName(input, tracker)
      val tparams                   = formatTParamSyms(input.methodSignature.typeParamSymbols)
      val (params, args) = if (input.methodSignature.parameterLists.isEmpty) {
        ("", "")
      } else {
        (
          input.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, input))).mkString("(", ", ", ")"),
          input.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodArg(_, input))).mkString("(", ", ", ")")
        )
      }
      result.addLine(s"def $displayName$tparams$params = $accessName$args${resOrCnv(input.adapted.interopType)}")
    }

    def doOutputVal(input: Input, tracker: PathTracker, interopType: Option[String]): Unit = {
      val (displayName, accessName) = displayNameAndAccessName(input, tracker)
      result.addLine(s"def $displayName = $accessName${resOrCnv(interopType)}")
    }

    def doOutputVar(input: Input, tpe: SdbType, tracker: PathTracker, interopType: Option[String]): Unit = {
      val (displayName, accessName) = displayNameAndAccessName(input, tracker)
      val unchangedType             = unchangedTypeFormatter(tpe)
      val iopType                   = interopType.getOrElse(interopTypeFormatter(tpe))
      result.addLine(s"def $displayName = $accessName.$$cnv[$iopType]")
      result.addLine(s"def ${displayName}_=(value: $iopType) = $accessName = value.$$cnv[$unchangedType]")
    }

    def outputCtorVal(input: Input.CtorParam, tracker: PathTracker): Unit = doOutputVal(input, tracker, input.adapted.interopType)

    def outputCtorVar(input: Input.CtorParam, tracker: PathTracker): Unit =
      doOutputVar(input, input.valueSignature.tpe, tracker, input.adapted.interopType)

    def outputDefValVar(a: Adapter.DefValVar, tracker: PathTracker): Unit = {
      a.input match {
        case i: Input.Def => outputDef(i, tracker)
        case i: Input.Val => doOutputVal(i, tracker, i.adapted.interopType)
        case i: Input.Var => doOutputVar(i, i.methodSignature.returnType, tracker, i.adapted.interopType)
      }
    }

    def outputAdapterObjProp(prop: Adapter.AdapterObjProp, tracker: PathTracker): Unit = {
      // assign a value to the adapter object; the adapter object can be referenced by its display name
      result.addLine(s"val ${prop.name} = ${prop.input.si.displayName}")
    }

    def outputNewDelegate(a: Adapter.NewDelegateDef, tracker: PathTracker): Unit = {
      val input                     = a.input
      val (params, args) = if (input.ctorParams.isEmpty) {
        ("", "")
      } else {
        def ctorParam(ct: CtorParam): String = s"${ct.si.displayName}: ${interopTypeFormatter(ct.valueSignature.tpe)}"
        def ctorArg(ct: CtorParam): String   = s"${ct.si.displayName}.$$cnv[${unchangedTypeFormatter(ct.valueSignature.tpe)}]"
        (
          input.ctorParams.map(ctorParam).mkString("(", ", ", ")"),
          input.ctorParams.map(ctorArg).mkString("(", ", ", ")")
        )
      }
      val tparams = formatTParamSyms(input.classSignature.typeParamSymbols)
      result.addLine(s"def newDelegate$tparams$params: ${tracker.typePath} = new ${tracker.delegatePath}$args")
    }

    def outputNewAdapter(a: Adapter.NewAdapterDef, tracker: PathTracker): Unit = {
      val input       = a.input
      val displayName = input.si.displayName
      result.openBlock(s"def newAdapter(delegate: ${tracker.typePath}): $displayName = new $displayName")
      result.addLine("override val $delegate = delegate")
      result.closeBlock()
    }

    def outputNested(a: Adapter.Container[_], tracker: PathTracker): Unit = {
      a.traits.values.foreach(a => outputTrait(a, tracker.nest(a)))
      a.objs.values.foreach(a => outputObj(a, tracker.nest(a), false))
    }

    def outputTrait(a: Adapter.Trait, tracker: PathTracker): Unit = {
      result.addLine("@JSExportAll")
      result.openBlock(s"trait ${a.name} extends InstanceAdapter[${tracker.typePath}]")
      val tr = tracker.nest(a)
      a.defs.values.foreach {
        case a: Adapter.DefValVar      => outputDefValVar(a, tr)
        case a: Adapter.AdapterObjProp => outputAdapterObjProp(a, tr)
        case a: Adapter.CtorParam if a.input.adapted.isAdapted =>
          a.input.mod match {
            case _: Input.CtorParamMod.Val => outputCtorVal(a.input, tr)
            case _: Input.CtorParamMod.Var => outputCtorVar(a.input, tr)
            case Input.CtorParamMod.Prv    =>
          }
      }

      outputNested(a, tracker)
      result.closeBlock()
    }

    def outputObj(a: Adapter.Obj, tracker: PathTracker, injectInstanceAdapterTrait: Boolean): Unit = {
      result.openBlock(s"object ${a.name} extends js.Object")

      if (injectInstanceAdapterTrait) {
        result.addLine("@JSExportAll")
        result.openBlock("trait InstanceAdapter[D]")
        result.addLine("val $delegate: D")
        result.closeBlock()
      }


      a.defs.values.foreach {
        case a: Adapter.DefValVar      => outputDefValVar(a, tracker)
        case a: Adapter.NewDelegateDef => outputNewDelegate(a, tracker)
        case a: Adapter.NewAdapterDef  => outputNewAdapter(a, tracker)
        case a: Adapter.AdapterObjProp => outputAdapterObjProp(a, tracker)
      }

      outputNested(a, tracker)
      result.closeBlock()
    }

    result.addLine("import scala.scalajs.js")
    result.addLine("import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}")
    result.addLine("import eu.swdev.scala.ts.adapter._")

    result.addLine(s"""@JSExportTopLevel("$adapterName")""")
    outputObj(root, PathTracker(s"_root_", s"_root_", false), root.hasTraits)

  }

  case class PathTracker(delegatePath: String, typePath: String, inTraitNotInObject: Boolean) {
    def nest(a: Adapter.Trait): PathTracker =
      if (inTraitNotInObject) {
        PathTracker("$delegate", s"$typePath#${a.name}", true)
      } else {
        PathTracker("$delegate", s"$typePath.${a.name}", true)
      }
    def nest(a: Adapter.Obj) =
      if (inTraitNotInObject) {
        PathTracker(s"$$delegate.${a.name}", s"$typePath#${a.name}", false)
      } else {
        PathTracker(s"$delegatePath.${a.name}", s"$typePath.${a.name}", false)
      }
  }

}
