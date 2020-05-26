import eu.swdev.scala.ts.ScalaTsPlugin

scalaTsGenerateDeclarationFile / logLevel := Level.Debug

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin)
  .settings(
    scalaVersion := "2.13.2",
    version := "0.1",
    name := "test"
  )
