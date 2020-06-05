import scala.sys.process.Process

scalaTsGenerateDeclarationFile / logLevel := Level.Debug

val npmReinstallAndTest = taskKey[Unit]("Reinstalls node modules and executes tests")

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin)
  .settings(
    scalaVersion := "2.13.2",
    version := "0.0.1",
    name := "scala-ts-e2e",
    organization := "eu.swdev",
    scalaTsModuleName := "scala-ts-mod",
    scalaTsModuleVersion := "0.0.2",
    scalaTsFilenamePrefix := "index",
    test := {
      // tests depend on scalaTsPackage
      scalaTsPackage.value
      val r = (
        Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
      )
      if (r != 0) {
        throw new MessageOnlyException("e2e tests failed")
      }
    },
    npmReinstallAndTest := {
      // tests depend on scalaTsPackage
      scalaTsPackage.value
      // when run by the ScriptedPlugin the node modules have to be reinstalled in the temporary project folder
      // -> remove the node_modules folder and run "npm i" before executing the tests
      val r = (
        Process("rm" :: "-rf" :: "node_modules" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) #&&
          Process("npm" :: "i" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) #&&
          Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
        )
      if (r != 0) {
        throw new MessageOnlyException("e2e tests failed")
      }
    },
  )
