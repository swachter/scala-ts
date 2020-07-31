import scala.sys.process.Process

ThisBuild / scalaVersion := "2.13.3"

val npmReinstallAndTest = taskKey[Unit]("Reinstalls node modules and executes tests")

val shared =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).settings(
    // the shared project contains classes that may use the @AdaptXXX annotations
    ScalaTsPlugin.adapterSettings
  ).jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
  ).jsSettings(
    // the shared project contains classes (i.e. Counter and Increment) that are referenced in the exported API of the client project
    // -> semantic db information is required for these classes
    // -> configure the semanticdb compiler plugin
    ScalaTsPlugin.semanticDbSettings
  )

val sharedJS = shared.js
val sharedJVM = shared.jvm

val client =
  project.enablePlugins(ScalaTsPlugin).settings(
    version := "0.0.1-SNAPSHOT",
    name := "scala-ts-e2e-adapter",
    organization := "eu.swdev",
    scalaTsModuleName := "scala-adapter",
    scalaTsValidate := true,
    scalaTsAdapterEnabled := true,
    (crossTarget in fastOptJS) := (baseDirectory in Compile).value / "target" / "node_module",
    (crossTarget in fullOptJS) := (baseDirectory in Compile).value / "target" / "node_module",
    //scalaTsChangeForkOptions := withDebugForkOptions(5005),
    test := {
      // tests depend on ScalaTs output
      scalaTsFastOpt.value
      val r = (
        Process("npm" :: "t" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) !
        )
      if (r != 0) {
        throw new MessageOnlyException("e2e tests failed")
      }
    },
    npmReinstallAndTest := {
      // tests depend on ScalaTs output
      scalaTsFastOpt.value
      // when run by the ScriptedPlugin the node modules have to be reinstalled in the temporary project folder
      // -> remove the node_modules folder and run "npm i" before executing the tests
      val r = (
        Process("rm" :: "-rf" :: "node_modules" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) #&&
          Process("rm" :: "-f" :: "package-lock.json" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) #&&
          Process("npm" :: "i" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) #&&
          Process("npm" :: "t" :: Nil, baseDirectory.value.getParentFile, "PATH" -> System.getenv("PATH")) !
        )
      if (r != 0) {
        throw new MessageOnlyException("e2e adapter tests failed")
      }
    },


  ).dependsOn(sharedJS)

lazy val root = project.in(file(".")).
  aggregate(client, sharedJS, sharedJVM).
  settings(
    name := "scala-ts-e2e-adapter",
    publish := {},
    publishLocal := {},
  )