package eu.swdev.scala.ts

import java.nio.file.Paths

import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import scala.meta.cli.Reporter
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.meta.{dialects, metacp}

object ScalaTsPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = ScalaJSPlugin

  object autoImport {
    val scalaTsOutputDir               = settingKey[File]("Directory where to put the TypeScript declaration file")
    val scalaTsFilenamePrefix          = settingKey[String]("Filename prefix of generated JavaScript and TypeScript declaration file")
    val scalaTsDeclarationFilename     = settingKey[String]("Filename of the TypeScript declaration file")
    val scalaTsDeclarationFile         = settingKey[File]("Generated TypeScript declaration file")
    val scalaTsPackageFile             = settingKey[File]("Generated package.json file")
    val scalaTsGenerateDeclarationFile = taskKey[File]("Generate typescript file")
    val scalaTsGeneratePackageFile     = taskKey[File]("Generate package.json file")
    val scalaTsPackage                 = taskKey[Unit]("Package all")
//    val scalaTsMetaCp                  = taskKey[Set[File]]("Generate semantic db information for dependencies")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaTsOutputDir := (baseDirectory in Compile).value / "target" / "web" / "js",
    scalaTsFilenamePrefix := name.value,
    artifactPath in fastOptJS in Compile := scalaTsOutputDir.value / (scalaTsFilenamePrefix.value + ".js"),
    artifactPath in fullOptJS in Compile := scalaTsOutputDir.value / (scalaTsFilenamePrefix.value + ".js"),
    (crossTarget in fullOptJS) := scalaTsOutputDir.value,
    (crossTarget in fastOptJS) := scalaTsOutputDir.value,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSUseMainModuleInitializer := false,
    scalaTsDeclarationFilename := scalaTsFilenamePrefix.value + ".d.ts",
    scalaTsDeclarationFile := scalaTsOutputDir.value / scalaTsDeclarationFilename.value,
    scalaTsPackageFile := scalaTsOutputDir.value / "package.json",
    scalaTsGenerateDeclarationFile := {
      val outputFile = scalaTsDeclarationFile.value
//      val metacp     = scalaTsMetaCp.value
      // define dependency on compile
      val compileVal = (compile in Compile).value
      val classDir      = (classDirectory in Compile).value

      val cp = (dependencyClasspath in Compile).value.files.map(AbsolutePath(_)).toList
      val symTab = GlobalSymbolTable(Classpath(cp), true)

      val semSrcs = SemSource.from(classDir, dialects.Scala212)

      val exports = semSrcs.flatMap(Analyzer.analyze)

      val output = Generator.generate(exports)

      println(s"###### writing output: $output")
      IO.write(outputFile, output, scala.io.Codec.UTF8.charSet)

      outputFile
    },
    scalaTsGeneratePackageFile := {
      val outputFile = scalaTsPackageFile.value
      val content =
        s"""
           |{
           |  "name": "${name.value}",
           |  "main": "${scalaTsFilenamePrefix.value}.js",
           |  "version": "${version.value}",
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
//    scalaTsMetaCp := {
//      val cp = (dependencyClasspath in Compile).value.files.map(AbsolutePath(_)).toList
//
//      // TODO use sbt logging
//      println(s"cp: $cp")
//
//      val settings = metacp
//        .Settings()
//        .withClasspath(meta.io.Classpath(cp))
//        .withVerbose(true)
//        .withIncludeJdk(true)
//
//      println(s"out: ${settings.out}")
//
//      // TODO: use sbt streams
//      val main = new meta.internal.metacp.Main(settings, Reporter())
//
//      val result = main.process()
//
//      result.classpath match {
//        case Some(cp) => cp.entries.map(_.toNIO.toFile).toSet
//        case None     => sys.error("semanticdb creation failed")
//      }
//
//    }
  )

}
