package eu.swdev.scala.ts

object BuiltIn {

  val builtInTypeNames: Map[Symbol, String] = Map(
    "java/lang/String#"    -> "string",
    "scala/Boolean#"       -> "boolean",
    "scala/Double#"        -> "number",
    "scala/Int#"           -> "number",
    "scala/Nothing#"       -> "never",
    "scala/Predef.String#" -> "string",
    "scala/Unit#"          -> "void",
  )

}
