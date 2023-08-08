import sbt.KeyRanks.APlusTask
import scala.jdk.CollectionConverters._
import scala.sys.process.Process

val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala3   = "3.4.0-RC1-bin-SNAPSHOT" // "3.3.0"
//val scala3   = "3.3.0"

val scalaVersions = List(scala213, scala212) // TODO: Scala 3 not (yet) working

val npmInstall = taskKey[Unit]("Install npm packages.")

ThisBuild / scalaVersion := scala213

scalaTsFastOpt / logLevel := Level.Info

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    crossScalaVersions := scalaVersions,
    version := "0.0.1-SNAPSHOT",
    name := "dts-cross",
    organization := "eu.swdev",
    scalaTsModuleName := {
      val scalaMajorMinor = CrossVersion.partialVersion(scalaVersion.value).map(t => s"${t._1}.${t._2}").get
      s"scala-$scalaMajorMinor-ts-mod"
    },
    scalaTsValidate := true,
    // enable adapter in order to include scala-ts-runtime library
    scalaTsAdapterEnabled := true,
    (fastOptJS / crossTarget) := (Compile / crossTarget).value / "node_module",
    (fullOptJS / crossTarget) := (Compile / crossTarget).value / "node_module",
    // scalaTsChangeForkOptions := withDebugForkOptions(5005),
    // the externalNpm task is executed by the ScalablyTypedConverterExternalNpmPlugin before the scala-ts module is generated
    // -> install dependencies; cf. https://scalablytyped.org/docs/plugin-no-bundler
    externalNpm := {
      val scalaMajorMinor = CrossVersion.partialVersion(scalaVersion.value).map(t => s"${t._1}.${t._2}").get
      val r = (
        Process("npm" :: "i" :: "-w" :: s"ws-scala-$scalaMajorMinor" :: "--omit=dev" :: Nil,
                baseDirectory.value,
                /* "PATH" -> System.getenv("PATH") */ ) !
      )
      if (r != 0) {
        throw new MessageOnlyException("Could not install nondev dependencies")
      }
      baseDirectory.value
    },
    npmInstall := Def
      .task {
        val scalaMajorMinor = CrossVersion.partialVersion(scalaVersion.value).map(t => s"${t._1}.${t._2}").get
        val cmd             = Seq("npm", "i", "-w", s"ws-scala-$scalaMajorMinor")
        val r = (
          Process(cmd, baseDirectory.value) !
        )
        val log = streams.value.log
        if (r != 0) {
          log.warn(s"Exit code of command '${cmd.mkString(" ")}' was non zero; maybe the error can be ignored - exitCode: $r")
        }
        baseDirectory.value
      }
      .dependsOn(scalaTsFastOpt)
      .value,
    test := Def
      .task {
        val scalaMajorMinor = CrossVersion.partialVersion(scalaVersion.value).map(t => s"${t._1}.${t._2}").get
        val r = (
          Process(Seq("npm", "t", "-w", s"ws-scala-$scalaMajorMinor"), baseDirectory.value) !
        )
        if (r != 0) {
          throw new MessageOnlyException("e2e tests failed")
        }
      }
      .dependsOn(npmInstall)
      .value,
    cleanFiles += baseDirectory.value / "node_modules",
    cleanFiles ++= {
      val scalaMajorMinor = CrossVersion.partialVersion(scalaVersion.value).map(t => s"${t._1}.${t._2}").get
      val workspaceDir    = baseDirectory.value / "src" / "test" / "ts" / s"ws-scala-$scalaMajorMinor"
      Seq(workspaceDir / "node_modules", workspaceDir / "dist")
    }
//    scalaTsChangeForkOptions := withDebugForkOptions(5005)
  )
