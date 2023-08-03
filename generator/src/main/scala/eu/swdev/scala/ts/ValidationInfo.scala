package eu.swdev.scala.ts

import scala.meta.tokens.Token

/**
  * Represents information that can be used to validate the TypeScript declaration file generation.
  *
  * The information is given in comments: A block comment that starts with ".d.ts:" or "adapter:" contains a
  * declaration file fragment or an adapter file fragment.
  * That fragment is either the result of processing the ScalaJS file that contains that comment or a group of ScalaJS files.
  * Groups of ScalaJS files can be formed by using an end of line comment that starts with "group:".
  *
  * Although being a test support type, the ValidationInfo type is defined in the main classes of the generator because
  * the ScalaTsPlugin depends on it to validate its generation result.
  *
  * In case that multiple ".d.ts:", "adapter:", or "group:" comments are given only the first is considered.
  */
sealed trait ValidationInfo

object ValidationInfo {

  case class Expected(string: String)                          extends ValidationInfo
  case class Group(group: String)                              extends ValidationInfo
  case class ExpectedAndGroup(expected: String, group: String) extends ValidationInfo

  val groupMarker   = "group:"
  val dtsMarker     = ".d.ts:"
  val adapterMarker = "adapter:"

  def apply(semSrc: SemSource, expectedMarker: String): Option[ValidationInfo] = {
    val optFragment = semSrc.tokens.collectFirst {
      case t: Token.Comment if t.value.trim.startsWith(expectedMarker) => t.value.trim.substring(expectedMarker.length).trim
    }
    val optGroup = semSrc.tokens.collectFirst {
      case t: Token.Comment if t.value.trim.startsWith(groupMarker) => t.value.trim.substring(groupMarker.length).trim
    }
    (optFragment, optGroup) match {
      case (Some(fragment), Some(group)) => Some(ExpectedAndGroup(fragment, group))
      case (Some(fragment), _)           => Some(Expected(fragment))
      case (_, Some(group))              => Some(Group(group))
      case _                             => None
    }
  }

  def dts(semSrc: SemSource)     = apply(semSrc, dtsMarker)
  def adapter(semSrc: SemSource) = apply(semSrc, adapterMarker)
}
