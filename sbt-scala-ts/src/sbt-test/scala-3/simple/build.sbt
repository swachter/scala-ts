import eu.swdev.scala.ts.ScalaTsPlugin

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin)
  .settings(
    scalaVersion := "3.4.0-RC1-bin-SNAPSHOT",
    version := "0.1.0",
    name := "simple-scala-3",
    scalaTsModuleName := "mod-name",
//    scalaTsChangeForkOptions := withDebugForkOptions(5005)
  )
