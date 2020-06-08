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
    val scalaTsModuleName = settingKey[String]("Name of the generated node module (default: project name)")
    val scalaTsModuleVersion =
      settingKey[String => String]("Transforms the project version into a node module version (default: identity function with check)")
    val scalaTsDialect = settingKey[Dialect]("Dialect of the ScalaJS sources (default: Scala213)")
    val scalaTsFastOpt = taskKey[Unit]("Generate node module including typescript declaration file based on the fastOptJS output")
    val scalaTsFullOpt = taskKey[Unit]("Generate node module including typescript declaration file based on the fullOptJS output")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaTsModuleName := name.value,
    scalaTsModuleVersion := semanticVersionCheck,
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
        scalaTsModuleVersion.value.apply(version.value),
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
        scalaTsModuleVersion.value.apply(version.value),
        scalaTsDialect.value,
        (classDirectory in Compile).value,
        (fullClasspath in Compile).value,
        streams.value.log
      )
    },
  )

  def semanticVersionCheck(v: String): String = {
    // regex copied from https://semver.org/
    if (v matches "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$") {
      v
    } else {
      throw new MessageOnlyException(
        "version '$v' is not a valid semantic version (cf. https://semver.org/); adjust the project version or set the scalaTsModuleVersion function")
    }
  }

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

    def inputInfo =
      exports
        .groupBy(_.getClass.getSimpleName)
        .toList
        .sortBy(_._1)
        .map {
          case (cn, lst) => s"$cn: ${lst.length}"
        }
        .mkString("{", ", ", "}")

    log.info(s"ScalaTs input : $inputInfo")

    val output = Generator.generate(exports, symTab)

    log.info(s"ScalaTs output: $dtsFile")

    log.debug(output)

    IO.write(dtsFile, output, scala.io.Codec.UTF8.charSet)

    val outputFile = jsPath.resolveSibling("package.json").toFile

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
