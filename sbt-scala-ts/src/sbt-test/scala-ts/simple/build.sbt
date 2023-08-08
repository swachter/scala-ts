import eu.swdev.scala.ts.ScalaTsPlugin

val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala3   = "3.4.0-RC1-bin-SNAPSHOT" // "3.3.0"
//val scala3   = "3.3.0"

val scalaVersions = List(scala3, scala213, scala212)

ThisBuild / scalaVersion := scala3

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin)
  .settings(
    crossScalaVersions := scalaVersions,
    scalaVersion := "2.13.11",
    version := "0.1.0",
    name := "simple-cross",
    scalaTsModuleName := "mod-name",
//    scalaTsChangeForkOptions := withDebugForkOptions(5006)
  )
