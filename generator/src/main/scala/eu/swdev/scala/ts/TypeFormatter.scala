package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.{
  BooleanConstant,
  ByteConstant,
  CharConstant,
  ConstantType,
  DoubleConstant,
  FloatConstant,
  IntConstant,
  LongConstant,
  NullConstant,
  ShortConstant,
  SingleType,
  StringConstant,
  Type,
  TypeRef,
  UnitConstant
}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => isb}

/**
  * Constructs a type formatter by combining formatter creators for known types, a formatter for built-in types, and a
  * formatter that formats every other type returning "any" as a last resort.
  */
class TypeFormatter(
    formatterCreatorsForKnownTypes: Seq[CTypeFormatter],
    exportedTypeName: Symbol => Option[String],
    symTab: SymbolTable
) extends (Type => String) {

  import TypeFormatter._

  override def apply(tpe: Type): String = formatType(tpe)

  def formatType(tpe: Type): String = formatter(tpe)

  def formatTypes(targs: Seq[isb.Type]) = formatTypeNames(targs.map(formatType))

  def isKnownOrBuiltIn(sym: Symbol): Boolean = knownOrBuiltInFormatter.isDefinedAt(TypeRef(isb.Type.Empty, sym, Seq.empty))

  val namedNativeTypes = mutable.Map.empty[Symbol, Nativeness.Named]

  val builtInFormatterCreator: CTypeFormatter = formatType => {
    case TypeRef(isb.Type.Empty, symbol, targs) if simpleBuiltInTypeNames.contains(symbol) =>
      simpleBuiltInTypeNames(symbol)
    case TypeRef(isb.Type.Empty, symbol, targs) if namedNativeTypes.contains(symbol) =>
      val tas = formatTypes(targs)
      namedNativeTypes(symbol) match {
        case Nativeness.Global(name)                => s"$name$tas"
        case Nativeness.Imported(module, "default") => s"${moduleName2Id(module)}_$tas"
        case Nativeness.Imported(module, name)      => s"${moduleName2Id(module)}.$name$tas"
      }
    case SingleType(isb.Type.Empty, symbol) if namedNativeTypes.contains(symbol) =>
      namedNativeTypes(symbol) match {
        case Nativeness.Global(name)                => s"$name"
        case Nativeness.Imported(module, "default") => s"${moduleName2Id(module)}_"
        case Nativeness.Imported(module, name)      => s"${moduleName2Id(module)}.$name"
      }
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
    case TypeRef(isb.Type.Empty, "scala/scalajs/js/`|`#", targs) =>
      s"${formatType(targs(0))} | ${formatType(targs(1))}"
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
    case TypeRef(isb.Type.Empty, symbol, targs) if symbol matches "scala/scalajs/js/Tuple\\d+#" =>
      targs.map(formatType).mkString("[", ", ", "]")

    case TypeRef(isb.Type.Empty, symbol, tArgs) if symTab.typeParamSymInfo(symbol).isDefined =>
      // it is a type parameter -> use its display name
      symTab.typeParamSymInfo(symbol).get.displayName

    case ConstantType(BooleanConstant(value)) => String.valueOf(value)
    case ConstantType(ByteConstant(value))    => String.valueOf(value)
    case ConstantType(CharConstant(value))    => "object" // ScalaJS represents char as object
    case ConstantType(DoubleConstant(value))  => String.valueOf(value)
    case ConstantType(FloatConstant(value))   => String.valueOf(value)
    case ConstantType(IntConstant(value))     => String.valueOf(value)
    case ConstantType(LongConstant(value))    => "object" // ScalaJS represents long as object
    case ConstantType(NullConstant())         => "null"
    case ConstantType(ShortConstant(value))   => String.valueOf(value)
    case ConstantType(StringConstant(value))  => s"'${escapeString(value)}'"
    case ConstantType(UnitConstant())         => "void"

  }

  val catchAllFormatterCreator: CTypeFormatter = formatType => {

    case TypeRef(isb.Type.Empty, symbol, tArgs) =>
      def tas = formatTypeNames(tArgs.map(formatType))
      exportedTypeName(symbol) match {
        case Some(str) => s"$str$tas"
        case None      => s"${nonExportedTypeName(symbol)}$tas"
      }

    case SingleType(isb.Type.Empty, symbol) =>
      exportedTypeName(symbol) match {
        case Some(str) => str
        case None      => nonExportedTypeName(symbol)
      }

    case _ => "any"
  }

  //
  //
  //

  val simpleBuiltInTypeNames: Map[Symbol, String] = Map(
    "java/lang/String#"        -> "string",
    "scala/Boolean#"           -> "boolean",
    "scala/Double#"            -> "number",
    "scala/Int#"               -> "number",
    "scala/Nothing#"           -> "never",
    "scala/Predef.String#"     -> "string",
    "scala/Unit#"              -> "void",
    "scala/scalajs/js/Any#"    -> "any",
    "scala/scalajs/js/Date#"   -> "Date",
    "scala/scalajs/js/Object#" -> "object",
    "scala/scalajs/js/RegExp#" -> "RegExp",
    "scala/scalajs/js/Symbol#" -> "symbol"
  )

  val knownOrBuiltInFormatterCreators = formatterCreatorsForKnownTypes ++ Seq(builtInFormatterCreator)
  val knownOrBuiltInFormatter         = knownOrBuiltInFormatterCreators.map(_.apply(this)).reduce(_ orElse _)

  val catchAllFormatter = catchAllFormatterCreator.apply(this)

  val formatter = knownOrBuiltInFormatter orElse catchAllFormatter

  def nonExportedTypeName(symbol: String): String = FullName.fromSymbol(symbol).str

}

object TypeFormatter {

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

  def moduleName2Id(moduleName: String) = {
    val namePart = moduleName.map {
      case c @ ('$' | '_')                                    => c
      case c if Character.isLetter(c) || Character.isDigit(c) => c
      case _                                                  => '_'
    }
    s"$$$namePart"
  }

  def formatTypeNames(args: Seq[String]): String = if (args.isEmpty) "" else args.mkString("<", ",", ">")

}
