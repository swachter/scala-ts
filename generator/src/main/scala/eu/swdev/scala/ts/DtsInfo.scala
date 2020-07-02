package eu.swdev.scala.ts

import scala.meta.tokens.Token

/**
  * Represents information that can be used to validate the TypeScript declaration file generation.
  *
  * The information is given in comments: A block comment that starts with ".d.ts:" contains a declaration file fragment
  * that is either the result of processing the ScalaJS file that contains that comment  or a group of ScalaJS files.
  * Groups of ScalaJS files can be formed by using specifying an end of line comment that starts with "group:".
  *
  * Although being a test support type, the DtsInfo type is defined in the main classes of the generator because
  * the ScalaTsPlugin depends on it to validate its generation result.
  *
  * In case that multiple ".d.ts:" or "group:" comments are given only the first is considered.
  */
sealed trait DtsInfo

object DtsInfo {

  case class Dts(dts: String)                        extends DtsInfo
  case class Group(group: String)                    extends DtsInfo
  case class DtsAndGroup(dts: String, group: String) extends DtsInfo

  val groupMarker = "group:"
  val dtsMarker   = ".d.ts:"

  def apply(semSrc: SemSource): Option[DtsInfo] = {
    val optDts = semSrc.tokens.collectFirst {
      case t: Token.Comment if t.value.trim.startsWith(dtsMarker) => t.value.trim.substring(dtsMarker.length).trim
    }
    val optGroup = semSrc.tokens.collectFirst {
      case t: Token.Comment if t.value.trim.startsWith(groupMarker) => t.value.trim.substring(groupMarker.length).trim
    }
    (optDts, optGroup) match {
      case (Some(dts), Some(group)) => Some(DtsAndGroup(dts, group))
      case (Some(dts), _)           => Some(Dts(dts))
      case (_, Some(group))         => Some(Group(group))
      case _                        => None
    }
  }
}
