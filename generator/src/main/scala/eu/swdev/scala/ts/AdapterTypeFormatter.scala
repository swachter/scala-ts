package eu.swdev.scala.ts

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

abstract class AdapterTypeFormatter(symTab: SymbolTable) extends (Type => String) {

  val typeParam: PTypeFormatter = {
    case TypeRef(Type.Empty, symbol, tArgs) if symTab.typeParamSymInfo(symbol).isDefined =>
      // it is a type parameter -> use its display name
      symTab.typeParamSymInfo(symbol).get.displayName
  }

  import AdapterTypeFormatter.simpleTypeNames

  val unchanged: CTypeFormatter = formatType => {
    case TypeRef(Type.Empty, sym, targs) if simpleTypeNames.contains(sym) => simpleTypeNames(sym)
    case TypeRef(Type.Empty, sym, targs) =>
      val ts = if (targs.isEmpty) {
        ""
      } else {
        targs.map(formatType).mkString("[", ",", "]")
      }
      s"_root_.${FullName.fromSymbol(sym)}$ts"
    case SingleType(Type.Empty, symbol)       => s"_root_.${FullName.fromSymbol(symbol)}"
    case ConstantType(BooleanConstant(value)) => String.valueOf(value)
    case ConstantType(ByteConstant(value))    => String.valueOf(value)
    case ConstantType(CharConstant(value))    => s"'${new Character(value.toChar)}'"
    case ConstantType(DoubleConstant(value))  => String.valueOf(value)
    case ConstantType(FloatConstant(value))   => String.valueOf(value)
    case ConstantType(IntConstant(value))     => String.valueOf(value)
    case ConstantType(LongConstant(value))    => s"${value}l"
    case ConstantType(NullConstant())         => "null"
    case ConstantType(ShortConstant(value))   => String.valueOf(value)
    case ConstantType(StringConstant(value))  => s"""${AdapterTypeFormatter.escapeScalaString(value)}"""
    case ConstantType(UnitConstant())         => "Unit"
  }

}

object AdapterTypeFormatter {

  def escapeScalaString(str: String): String = {
    str.flatMap {
      case '\n'                      => "\\n"
      case '\r'                      => "\\r"
      case '\t'                      => "\\t"
      case '\\'                      => "\\"
      case c if c >= ' ' && c <= 126 => c.toString
      case c                         => f"\\u$c%04x"
    }
  }

  val simpleTypeNames: Map[Symbol, String] = Map(
    "java/lang/String#"    -> "String",
    "scala/Boolean#"       -> "Boolean",
    "scala/Byte#"          -> "Byte",
    "scala/Double#"        -> "Double",
    "scala/Float#"         -> "Float",
    "scala/Int#"           -> "Int",
    "scala/Nothing#"       -> "Nothing",
    "scala/Predef.String#" -> "String",
    "scala/Short#"         -> "Short",
    "scala/Unit#"          -> "Unit",
  )

}

class UnchangedTypeFormatter(symTab: SymbolTable) extends AdapterTypeFormatter(symTab) {
  val formatter                       = typeParam orElse unchanged(this)
  override def apply(t: Type): String = formatter(t)
}

class InteropTypeFormatter(symTab: SymbolTable) extends AdapterTypeFormatter(symTab) {

  val formatter = typeParam orElse Function.unlift(changed) orElse unchanged(this)

  def needsConverter(t: Type): Boolean = changed(t).isDefined

  override def apply(t: Type): String = formatter(t)

  private def changed(tpe: Type): Option[String] = {
    for {
      TypeRef(_, _, targs) <- Option(tpe)
      normalizedSym        <- tpe.typeSymbol(symTab)
      replaced             <- simpleReplacements.get(normalizedSym).orElse(complexReplacment.lift(normalizedSym))
    } yield {
      val args = if (targs.isEmpty) "" else targs.map(apply).mkString("[", ",", "]")
      s"$replaced$args"
    }
  }

  val simpleReplacements = Map(
    "java/util/Date#"                  -> "_root_.scala.scalajs.js.Date",
    "scala/Array#"                     -> "_root_.scala.scalajs.js.Array",
    "scala/Option#"                    -> "_root_.scala.scalajs.js.UndefOr",
    "scala/collection/immutable/List#" -> "_root_.scala.scalajs.js.Array",
    "scala/concurrent/Future#"         -> "_root_.scala.scalajs.js.Promise"
  )

  val complexReplacment: PartialFunction[String, String] = {
    case sym if sym matches "scala/Function\\d+#" => s"_root_.scala.scalajs.js.Function${sym.dropRight(1).substring(14)}"
    case sym if sym matches "scala/Tuple\\d+#"    => s"_root_.scala.scalajs.js.Tuple${sym.dropRight(1).substring(11)}"
  }
}
