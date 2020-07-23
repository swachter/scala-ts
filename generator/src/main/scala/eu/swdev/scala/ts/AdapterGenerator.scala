package eu.swdev.scala.ts

import scala.meta.internal.semanticdb.{SingleType, SymbolInformation, Type, TypeRef, ValueSignature}
import scala.meta.internal.symtab.SymbolTable

object AdapterGenerator {

  def generate(inputs: List[Input.Defn], symTab: SymbolTable, result: Result): Unit = {

    val topLevelExports = Analyzer.topLevel(inputs)

    val unchangedTypeFormatter = new UnchangedTypeFormatter(symTab)
    val interopTypeFormatter = new InteropTypeFormatter(symTab)

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
        s".convert[${unchangedTypeFormatter(vs.tpe)}]"
      } else {
        ""
      }
      s"${si.displayName}$conv"
    }

    def memberDef(i: Input.Def): Unit = {
      i.name.foreach {
        case NameAnnot.JsNameWithString(s) => result.addLine(s"""@JSName("$s")""")
        case _                             =>
      }
      val tparams = formatTParamSyms(i.methodSignature.typeParamSymbols)
      val (params, args) = if (i.methodSignature.parameterLists.isEmpty) {
        ("", "")
      } else {
        (
          i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodParam(_, i))).mkString("(", ", ", ")"),
          i.methodSignature.parameterLists.flatMap(_.symlinks.map(formatMethodArg(_, i))).mkString("(", ", ", ")")
        )
      }
      result.addLine(s"def ${i.si.displayName}$tparams$params = result(delegate.${i.si.displayName}$args")
    }

    def memberVal(i: Input.Val): Unit = {}

    def memberVar(i: Input.Var): Unit = {}

    def topLevelObj(name: String, i: Input.Obj): Unit = {
      result.addLine(s"""@JSExportTopLevel("$name$$a")""")
      result.addLine(s"@JSExportAll")
      result.openBlock(s"object ${adapterId(i.si)}")
      result.addLine(s"val delegate = ${FullName(i.si).toString.replace("$", "")}")
      i.member.foreach {
        case i: Input.Def => memberDef(i)
        case i: Input.Val => memberVal(i)
        case i: Input.Var => memberVar(i)
        case _            =>
      }
      result.closeBlock()
    }

    result.addLine("import scala.scalajs.js")
    result.addLine("import eu.swdev.scala.ts.adapter._")
    result.addLine("import js.JSConverters._")

    result.openBlock("object Adapter")

    topLevelExports.foreach {
      case TopLevelExport(n, i: Input.Def) => //exportDef(n, i)
      case TopLevelExport(n, i: Input.Val) => //exportVal(n, i)
      case TopLevelExport(n, i: Input.Var) => //exportVar(n, i)
      case TopLevelExport(n, i: Input.Obj) => topLevelObj(n, i)
      case TopLevelExport(n, i: Input.Cls) => //exportCls(n, i)
    }

    result.closeBlock()
  }

  private def adapterId(si: SymbolInformation): String = FullName(si).toString.replace('.', '_')
}
