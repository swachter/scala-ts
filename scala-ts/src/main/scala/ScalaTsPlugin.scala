package eu.swdev.scala.ts

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}

object ScalaTsPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = ScalaJSPlugin

  object autoImport {
    val scalaTsOutputDir     = settingKey[File]("Directory where to put the TypeScript declaration file (default: target/node_module)")
    val scalaTsModuleName    = settingKey[String]("Name of the generated node module (default: project name)")
    val scalaTsModuleVersion = settingKey[String]("Version of the generated node module (default: project version)")
    val scalaTsFilenamePrefix =
      settingKey[String]("Filename prefix of generated JavaScript and TypeScript declaration file (default: project name)")
    val scalaTsDialect                 = settingKey[Dialect]("Dialect of the ScalaJS sources; default: scala.meta.dialects.Scala213")
    val scalaTsGenerateDeclarationFile = taskKey[File]("Generate TypeScript declaration file")
    val scalaTsGeneratePackageFile     = taskKey[File]("Generate package.json file")
    val scalaTsPackage                 = taskKey[Unit]("Package all - generate the node module")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaTsOutputDir := (baseDirectory in Compile).value / "target" / "node_module",
    scalaTsModuleName := name.value,
    scalaTsModuleVersion := version.value,
    scalaTsFilenamePrefix := name.value,
    scalaTsDialect := dialects.Scala213,
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.10" cross CrossVersion.full),
    scalacOptions += "-Yrangepos",
    scalacOptions += "-P:semanticdb:text:on",
    artifactPath in fastOptJS in Compile := scalaTsOutputDir.value / (scalaTsFilenamePrefix.value + ".js"),
    artifactPath in fullOptJS in Compile := scalaTsOutputDir.value / (scalaTsFilenamePrefix.value + ".js"),
    (crossTarget in fullOptJS) := scalaTsOutputDir.value,
    (crossTarget in fastOptJS) := scalaTsOutputDir.value,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSUseMainModuleInitializer := false,
    scalaTsGenerateDeclarationFile := {
      val outputFile = scalaTsOutputDir.value / (scalaTsFilenamePrefix.value + ".d.ts")
      // define dependency on compile
      // -> ensures that compilation is up to date
      val compileVal = (compile in Compile).value
      val classDir   = (classDirectory in Compile).value

      val cp     = (fullClasspath in Compile).value.files.map(AbsolutePath(_)).toList
      val symTab = GlobalSymbolTable(Classpath(cp), true)

      val semSrcs = SemSource.from(classDir, scalaTsDialect.value)

      val exports = semSrcs.sortBy(_.td.uri).flatMap(Analyzer.analyze)

      val output = Generator.generate(exports, symTab)

      val log = streams.value.log

      val exportInfo = exports
        .groupBy(_.getClass.getSimpleName)
        .toList
        .sortBy(_._1)
        .map {
          case (cn, lst) => f"  # $cn%-5s: ${lst.length}"
        }
        .mkString("\n")

      log.info(s"type declaration file: $outputFile")
      log.info(s"$exportInfo")

      log.debug(output)

      IO.write(outputFile, output, scala.io.Codec.UTF8.charSet)

      outputFile
    },
    scalaTsGeneratePackageFile := {
      val outputFile = scalaTsOutputDir.value / "package.json"
      val content =
        s"""
           |{
           |  "name": "${scalaTsModuleName.value}",
           |  "main": "${scalaTsFilenamePrefix.value}.js",
           |  "version": "${scalaTsModuleVersion.value}",
           |  "type": "module"
           |}
        """.stripMargin
      IO.write(outputFile, content, scala.io.Codec.UTF8.charSet)
      outputFile
    },
    scalaTsPackage := Def
      .sequential(
        (fastOptJS in Compile),
        scalaTsGenerateDeclarationFile,
        scalaTsGeneratePackageFile,
      )
      .value,
  )

}
