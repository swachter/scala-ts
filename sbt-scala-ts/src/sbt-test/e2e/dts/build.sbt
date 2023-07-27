import scala.sys.process.Process

scalaTsFastOpt / logLevel := Level.Info

lazy val root = (project in file("."))
  .enablePlugins(ScalaTsPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    scalaVersion := "2.13.11",
    version := "0.0.1-SNAPSHOT",
    name := "scala-ts-e2e-dts",
    organization := "eu.swdev",
    scalaTsModuleName := "scala-ts-mod",
    scalaTsValidate := true,
    // enable adapter in order to include scala-ts-runtime library
    scalaTsAdapterEnabled := true,
    (crossTarget in fastOptJS) := (baseDirectory in Compile).value / "target" / "node_module",
    (crossTarget in fullOptJS) := (baseDirectory in Compile).value / "target" / "node_module",
    // scalaTsChangeForkOptions := withDebugForkOptions(5005),
    // the externalNpm task is executed by the ScalablyTypedConverterExternalNpmPlugin before the scala-ts module is generated
    // -> install dependencies; cf. https://scalablytyped.org/docs/plugin-no-bundler
    externalNpm := {
      val r = (
        Process("npm" :: "i" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
        )
      if (r != 0) {
        throw new MessageOnlyException("Could not install dependencies")
      }
      baseDirectory.value
    },
    test := Def.task {
      val r = (
        Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
        )
      if (r != 0) {
        throw new MessageOnlyException("e2e tests failed")
      }
    }.dependsOn(scalaTsFastOpt, externalNpm).value,
    cleanFiles += baseDirectory.value / "node_modules"
//    scalaTsChangeForkOptions := withDebugForkOptions(5005)
  )
