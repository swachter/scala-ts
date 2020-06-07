package eu.swdev.scala.ts

import eu.swdev.scala.ts

import scala.meta.internal.symtab.SymbolTable

case class Alias(e: Input.Alias) {
  def fullName: FullName = FullName(e.si.symbol)
  def simpleName = fullName.last
  def typeParamDisplayNames(symTab: SymbolTable): Seq[String] = e.si.typeParamDisplayNames(symTab)
  def rhs = e.typeSignature.lowerBound
}
