package eu.swdev.scala.ts

import eu.swdev.scala.ts.Input.CtorParam

import scala.meta.internal.semanticdb.{SymbolInformation, ValueSignature, Type => SdbType}
import scala.meta.internal.symtab.SymbolTable

object AdapterGenerator {

  def generate(inputs: List[Input.Defn], symTab: SymbolTable, result: Result): Unit = {

    val root = AdapterObject(inputs)

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

    def displayNameAndAccessName(input: Input, outerName: List[String]): (String, String) = {
      val displayName = input.si.displayName
      val accessName  = (displayName :: outerName).reverse.mkString(".")
      (displayName, accessName)
    }

    def outputDef(a: Adaption.Def, outerName: List[String]): Unit = {
      val input                     = a.input
      val (displayName, accessName) = displayNameAndAccessName(input, outerName)
      val tparams                   = formatTParamSyms(input.methodSignature.typeParamSymbols)
      val (params, args) = if (input.methodSignature.parameterLists.isEmpty) {
        ("", "")
      } else {
        (
          input.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, input))).mkString("(", ", ", ")"),
          input.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodArg(_, input))).mkString("(", ", ", ")")
        )
      }
      result.addLine(s"def $displayName$tparams$params = $$res($accessName$args)")
    }

    def doOutputVal(input: Input, outerName: List[String]): Unit = {
      val (displayName, accessName) = displayNameAndAccessName(input, outerName)
      result.addLine(s"def $displayName = $$res($accessName)")
    }

    def doOutputVar(input: Input, tpe: SdbType, outerName: List[String]): Unit = {
      val (displayName, accessName) = displayNameAndAccessName(input, outerName)
      val unchangedType             = unchangedTypeFormatter(tpe)
      val interopType               = interopTypeFormatter(tpe)
      result.addLine(s"def $displayName = $$res($$delegate.${input.si.displayName})")
      result.addLine(s"def ${displayName}_=(value: $interopType) = $accessName = value.$$cnv[$unchangedType]")
    }

    def outputVal(a: Adaption.Val, outerName: List[String]): Unit = doOutputVal(a.input, outerName)

    def outputVar(a: Adaption.Var, outerName: List[String]): Unit = doOutputVar(a.input, a.input.methodSignature.returnType, outerName)

    def outputCtorVal(input: Input.CtorParam, outerName: List[String]): Unit = doOutputVal(input, outerName)

    def outputCtorVar(input: Input.CtorParam, outerName: List[String]): Unit = doOutputVar(input, input.valueSignature.tpe, outerName)

    def outputNewInstance(a: Adaption.NewInstance, outerName: List[String]): Unit = {
      val input                     = a.input
      val (displayName, accessName) = displayNameAndAccessName(input, outerName)
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
      // TODO does not work for nested classes; the instance can not be created by "new FullName"; it must be "new $delegate.DisplayName"
      result.addLine(s"def newInstance$tparams$params = new _root_.${FullName(input.si)}$args")
    }

    def outputNewAdapter(a: Adaption.NewAdapter, outerName: List[String]): Unit = {
      val input       = a.input
      val displayName = input.si.displayName
      // TODO does not work for nested classes; the delegate must be of type "$delegate.DisplayName"
      result.openBlock(s"def newAdapter(delegate: _root_.${FullName(input.si)}): $displayName = new $displayName")
      result.addLine("$delegate = delegate")
      result.closeBlock()
    }

    def outputTrait(a: Adaption.Trait): Unit = {
      val input = a.input
      result.addLine("@JSExportAll")
      result.openBlock(s"trait ${input.si.displayName} extends InstanceAdapter[_root_.${FullName(input.si)}]")
      input.ctorParams.foreach { ctp =>
        if (ctp.adapted.isAdapted) {
          ctp.mod match {
            case _: Input.CtorParamMod.Val => outputCtorVal(ctp, List("$delegate"))
            case _: Input.CtorParamMod.Var => outputCtorVar(ctp, List("$delegate"))
            case Input.CtorParamMod.Prv    =>
          }
        }
      }
      input.member.foreach {
        case i: Input.Def if i.adapted.isAdapted => outputDef(Adaption.Def(i), List("$delegate"))
        case i: Input.Val if i.adapted.isAdapted => outputVal(Adaption.Val(i), List("$delegate"))
        case i: Input.Var if i.adapted.isAdapted => outputVar(Adaption.Var(i), List("$delegate"))
        case _                                   =>
      }
      result.closeBlock()
    }

    result.addLine("import scala.scalajs.js")
    result.addLine("import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}")
    result.addLine("import eu.swdev.scala.ts.adapter._")

    val adapterName = "Adapter"

    result.addLine(s"""@JSExportTopLevel("$adapterName")""")
    result.openBlock(s"object $adapterName extends js.Object")
    result.addLine("@JSExportAll")
    result.openBlock("trait InstanceAdapter[D]")
    result.addLine("val $delegate: D")
    result.closeBlock()

    def outputAdapter(ao: AdapterObject, outerName: List[String]): Unit = {

      ao.methods.values.foreach {
        case a: Adaption.Def         => outputDef(a, outerName)
        case a: Adaption.Val         => outputVal(a, outerName)
        case a: Adaption.Var         => outputVar(a, outerName)
        case a: Adaption.NewInstance => outputNewInstance(a, outerName)
        case a: Adaption.NewAdapter  => outputNewAdapter(a, outerName)
      }

      ao.traits.values.foreach {
        case a: Adaption.Trait => outputTrait(a)
      }

      ao.nested.foreach {
        case (name, ao) =>
          result.openBlock(s"object $name extends js.Object")
          outputAdapter(ao, name :: outerName)
          result.closeBlock()
      }

    }

    outputAdapter(root, Nil)

    result.closeBlock()
  }

  private def adapterId(si: SymbolInformation): String = FullName(si).toString.replace('.', '_')
}
