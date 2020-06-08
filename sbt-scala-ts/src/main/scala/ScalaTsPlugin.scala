package eu.swdev.scala.ts

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.internal.util.ManagedLogger

import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}

object ScalaTsPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = ScalaJSPlugin

  object autoImport {
    val scalaTsModuleName    = settingKey[String]("Name of the generated node module (default: project name)")
    val scalaTsModuleVersion = settingKey[String]("Version of the generated node module (default: project version)")
    val scalaTsDialect       = settingKey[Dialect]("Dialect of the ScalaJS sources (default: Scala213)")
    val scalaTsFastOpt       = taskKey[Unit]("Generate node module including typescript declaration file based on the fastOptJS output")
    val scalaTsFullOpt       = taskKey[Unit]("Generate node module including typescript declaration file based on the fullOptJS output")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaTsModuleName := name.value,
    scalaTsModuleVersion := version.value,
    scalaTsDialect := dialects.Scala213,
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.10" cross CrossVersion.full),
    scalacOptions += "-Yrangepos",
    scalacOptions += "-P:semanticdb:text:on",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSUseMainModuleInitializer := false,
    scalaTsFastOpt := {
      (Compile / fastOptJS).value
      generateFiles(
        (artifactPath in fastOptJS in Compile).value,
        scalaTsModuleName.value,
        scalaTsModuleVersion.value,
        scalaTsDialect.value,
        (classDirectory in Compile).value,
        (fullClasspath in Compile).value,
        streams.value.log
      )
    },
    scalaTsFullOpt := {
      (Compile / fullOptJS).value
      generateFiles(
        (artifactPath in fullOptJS in Compile).value,
        scalaTsModuleName.value,
        scalaTsModuleVersion.value,
        scalaTsDialect.value,
        (classDirectory in Compile).value,
        (fullClasspath in Compile).value,
        streams.value.log
      )
    },
  )

  def generateFiles(
      jsFile: File,
      moduleName: String,
      moduleVersion: String,
      dialect: Dialect,
      compileClassDir: File,
      compileFullClasspath: Keys.Classpath,
      log: ManagedLogger
  ): Unit = {
    val jsPath     = jsFile.toPath
    val jsFileName = jsPath.getFileName.toString

    val idx               = jsFileName.lastIndexOf('.')
    val dtsFileNameString = s"${if (idx >= 0) jsFileName.substring(0, idx) else jsFileName}.d.ts"
    val dtsFile           = jsPath.resolveSibling(dtsFileNameString).toFile

    val cp     = compileFullClasspath.files.map(AbsolutePath(_)).toList
    val symTab = GlobalSymbolTable(Classpath(cp), true)

    val semSrcs = SemSource.from(compileClassDir, dialect)

    val exports = semSrcs.sortBy(_.td.uri).flatMap(Analyzer.analyze(_, symTab))

    val output = Generator.generate(exports, symTab)

    val exportInfo = exports
      .groupBy(_.getClass.getSimpleName)
      .toList
      .sortBy(_._1)
      .map {
        case (cn, lst) => f"  # $cn%-5s: ${lst.length}"
      }
      .mkString("\n")

    log.info(s"type declaration file: $dtsFile")
    log.info(s"$exportInfo")

    log.debug(output)

    IO.write(dtsFile, output, scala.io.Codec.UTF8.charSet)

    val outputFile = jsPath.resolveSibling("package.json").toFile
    if (!(moduleVersion matches """^\d+\.\d+\.\d+$$""")) {
      throw new MessageOnlyException(
        s"node module version '$moduleVersion' is not a valid semantic version (3 dot separated numbers); adjust the project version or set the scalaTsModuleVersion")
    }
    val content =
      s"""
         |{
         |  "name": "$moduleName",
         |  "main": "$jsFileName",
         |  "version": "$moduleVersion",
         |  "type": "module"
         |}
         |""".stripMargin
    IO.write(outputFile, content, scala.io.Codec.UTF8.charSet)

  }

}
