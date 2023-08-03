import scala.sys.process.Process

ThisBuild / scalaVersion := "3.4.0-RC1-bin-SNAPSHOT"

val npmInstallTask = taskKey[Unit]("Execute the 'npm install' command")

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(
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
      name := "adapter-scala-3",
      organization := "eu.swdev",
      scalaTsModuleName := "scala-adapter",
      scalaTsValidate := true,
      scalaTsAdapterEnabled := true,
      (fastOptJS / crossTarget) := (Compile / baseDirectory).value / "target" / "node_module",
      (fullOptJS / crossTarget) := (Compile / baseDirectory).value / "target" / "node_module",
      //scalaTsChangeForkOptions := withDebugForkOptions(5005),
      npmInstallTask := {
        val r = (
          Process("npm" :: "i" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) !
        )
        if (r != 0) {
          throw new MessageOnlyException("Could not install dependencies")
        }
      },
      test := Def
        .task {
          val r = (
            Process("npm" :: "t" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) !
          )
          if (r != 0) {
            throw new MessageOnlyException("e2e tests failed")
          }
        }
        .dependsOn(scalaTsFastOpt, npmInstallTask)
        .value,
    )

lazy val root = project
  .in(file("."))
  .aggregate(client, shared.js, shared.jvm)
  .settings(
    name := "scala-ts-e2e-adapter",
    publish := {},
    publishLocal := {},
    cleanFiles += baseDirectory.value / "node_modules"
  )
