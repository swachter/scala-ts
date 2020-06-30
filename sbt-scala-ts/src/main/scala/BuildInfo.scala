package eu.swdev.scala.ts

import java.util.Properties

import sbt.internal.util.MessageOnlyException

object BuildInfo {
  def version: String = props.getProperty("version")

  private lazy val props: Properties = {
    val props = new Properties()
    val path = "scala-ts.properties"
    val classloader = this.getClass.getClassLoader
    Option(classloader.getResourceAsStream(path)) match {
      case Some(stream) => props.load(stream)
      case None => throw new MessageOnlyException(s"can not determine ScalaTsPlugin version; missing resource: $path")
    }
    props
  }
}
