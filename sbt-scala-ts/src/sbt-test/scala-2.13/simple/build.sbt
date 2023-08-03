import eu.swdev.scala.ts.ScalaTsPlugin

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin)
  .settings(
    scalaVersion := "2.13.11",
    version := "0.1.0",
    name := "simple",
    scalaTsModuleName := "mod-name",
//    scalaTsChangeForkOptions := withDebugForkOptions(5006)
  )
