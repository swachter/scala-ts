package eu.swdev.scala.ts

import eu.swdev.scala.ts.TypeFormatter.escapeTypeScriptString

import scala.meta.internal.semanticdb.{BooleanConstant, ByteConstant, CharConstant, ConstantType, DoubleConstant, FloatConstant, IntConstant, LongConstant, NullConstant, ShortConstant, SingleType, StringConstant, Type, TypeRef, UnitConstant}
import scala.meta.internal.symtab.SymbolTable

abstract class AdapterTypeFormatter(symTab: SymbolTable) extends (Type => String) {

  val typeParam: PTypeFormatter = {
    case TypeRef(Type.Empty, symbol, tArgs) if symTab.typeParamSymInfo(symbol).isDefined =>
      // it is a type parameter -> use its display name
      symTab.typeParamSymInfo(symbol).get.displayName
  }

  val unchanged: CTypeFormatter = formatType => {
    case TypeRef(Type.Empty, sym, targs) =>
      val ts = if (targs.isEmpty) {
        ""
      } else {
        targs.map(formatType).mkString("[", ",", "]")
      }
      s"${FullName.fromSymbol(sym)}$ts"
    case SingleType(Type.Empty, symbol) =>
      s"${FullName.fromSymbol(symbol)}"
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
}

class UnchangedTypeFormatter(symTab: SymbolTable) extends AdapterTypeFormatter(symTab) {
  val formatter = typeParam orElse unchanged(this)
  override def apply(t: Type): String = formatter(t)
}

class InteropTypeFormatter(symTab: SymbolTable) extends AdapterTypeFormatter(symTab) {

  val withoutConverterTf: PTypeFormatter = {
    case TypeRef(Type.Empty, symbol, targs) if symbol matches "scala/Function\\d+#" =>
      val args = targs.map(apply).mkString("[", ",", "]")
      s"js.Function${targs.size - 1}$args"
    case TypeRef(Type.Empty, "scala/Option#", targs) =>
      s"js.UndefOr[${apply(targs(0))}]"
  }

  val withConverterTf: PTypeFormatter = {
    case TypeRef(Type.Empty, "scala/Array#", targs) =>
      s"js.Array[${apply(targs(0))}]"
  }

  val formatter = typeParam orElse withoutConverterTf orElse withConverterTf orElse unchanged(this)

  def needsConverter(t: Type): Boolean = withConverterTf.isDefinedAt(t)

  override def apply(t: Type): String = formatter(t)
}