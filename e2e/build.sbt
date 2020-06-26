import scala.sys.process.Process

scalaTsFastOpt / logLevel := Level.Info

val npmReinstallAndTest = taskKey[Unit]("Reinstalls node modules and executes tests")

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    scalaVersion := "2.13.2",
    version := "0.0.1-SNAPSHOT",
    name := "scala-ts-e2e",
    organization := "eu.swdev",
    scalaTsModuleName := "scala-ts-mod",
    (crossTarget in fastOptJS) := (baseDirectory in Compile).value / "target" / "node_module",
    (crossTarget in fullOptJS) := (baseDirectory in Compile).value / "target" / "node_module",
    // scalaTsChangeForkOptions := withDebugForkOptions(5005),
    test := {
      // tests depend on ScalaTs output
      scalaTsFastOpt.value
      val r = (
        Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
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
        Process("rm" :: "-rf" :: "node_modules" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) #&&
          Process("rm" :: "package-lock.json" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) #&&
          Process("npm" :: "i" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) #&&
          Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
        )
      if (r != 0) {
        throw new MessageOnlyException("e2e tests failed")
      }
    },
    externalNpm := {
      Process(Seq("npm", "i"), baseDirectory.value).!
      baseDirectory.value
    },
  )
