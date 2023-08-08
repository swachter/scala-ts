import scala.sys.process.Process

val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala3   = "3.4.0-RC1-bin-SNAPSHOT" // "3.3.0"
//val scala3   = "3.3.0"

val scalaVersions = List(scala213, scala212) // TODO: Scala 3 not (yet) working

ThisBuild / scalaVersion := scala213

val npmInstall = taskKey[Unit]("Execute the 'npm install' command")

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(
      crossScalaVersions := scalaVersions,
      // add annotations dependency
      ScalaTsPlugin.crossProject.settings
    )
    .jvmSettings(
      libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
    )
    .jsSettings(
      // adds semantic db settings
      ScalaTsPlugin.crossProject.jsSettings
    )

lazy val client =
  project
    .enablePlugins(ScalaTsPlugin)
    .dependsOn(shared.js)
    .settings(
      crossScalaVersions := scalaVersions,
      name := "adapter-cross",
      organization := "eu.swdev",
      scalaTsModuleName := {
        val scalaMajorMinor = CrossVersion.partialVersion(scalaVersion.value).map(t => s"${t._1}.${t._2}").get
        s"scala-$scalaMajorMinor-ts-mod"
      },
      scalaTsValidate := true,
      scalaTsAdapterEnabled := true,
      (fastOptJS / crossTarget) := (Compile / crossTarget).value / "node_module",
      (fullOptJS / crossTarget) := (Compile / crossTarget).value / "node_module",
      //scalaTsChangeForkOptions := withDebugForkOptions(5005),
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
    )

lazy val root = project
  .in(file("."))
  .aggregate(client, shared.js, shared.jvm)
  .settings(
    name := "adapter-cross",
    crossScalaVersions := Nil,
//    crossScalaVersions := scalaVersions,
    publish := {},
    publishLocal := {},
  )
