/*
  Release process:

    - set release version in build.sbt and in readme.md

    sbt> reload
    sbt> +publishLocal
    sbt> +test
    sbt> scripted

    > git commit -am "v..."
    > git tag -a "v..."
    > git push --follow-tags

    sbt> +publish

    - set next snapshot version in build.sbt and in e2e/project/plugins.sbt
 */

import sbt.internal.inc.ScalaInstance

import scala.sys.process.Process

val scalaMetaVersion = "4.3.10"
// the ScalaJS version the ScalaTsPlugin depends upon
// (this build also uses the ScalaJS plugin; that version is configure in project/plugins.sbt)
val scalaJsVersion = "1.1.1"

lazy val scala212 = "2.12.11"
lazy val scala213 = "2.13.2"

lazy val commonSettings = Seq(
  organization := "eu.swdev",
  version := "0.10-SNAPSHOT",
  bintrayPackageLabels := Seq("sbt","plugin"),
  bintrayVcsUrl := Some("""https://github.com/swachter/scala-ts.git"""),
  bintrayOrganization := None, // TODO: what is the organization for
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
)

lazy val generator = project.in(file("generator"))
  .settings(commonSettings: _*)
  .settings(
    name := "scala-ts-generator",
    description := "library for generating TypeScript declaration files from ScalaJS sources",
    crossScalaVersions := List(scala212, scala213),
    publishMavenStyle := true,
    bintrayRepository := "maven",
    // activate the SemanticDB compiler plugin in the test configuraion only
    // -> add the compiler plugin dependency
    // -> set autoCompilerPlugins := false -> no "-Xplugin=..." Scalac option is added automatically
    // -> add the necessary Scalac options manually in the test configuration
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.10" cross CrossVersion.full),
    autoCompilerPlugins := false,
    ivyConfigurations += Configurations.CompilerPlugin,
    scalacOptions in Test ++= Classpaths.autoPlugins(update.value, Seq(), ScalaInstance.isDotty(scalaVersion.value)),
    scalacOptions in Test += "-Yrangepos",
    scalacOptions in Test += "-P:semanticdb:text:on",
    libraryDependencies += "org.scalameta" %% "scalameta" % scalaMetaVersion,
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.2" % "test",
  )

lazy val explore = project
  .in(file("explore"))
  .settings(
    name := "explore",
    scalaVersion := scala213,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    test := {
      (Compile / fastOptJS).value
      Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
    }
  ).enablePlugins(ScalaJSPlugin)

lazy val plugin = project
  .in(file("sbt-scala-ts"))
  .enablePlugins(ScriptedPlugin)
  .dependsOn(generator)
  .settings(commonSettings: _*)
  .settings(
    name := s"sbt-scala-ts",
    description := "SBT plugin for generating TypeScript declaration files for ScalaJS sources",
    resourceGenerators.in(Compile) += Def.task {
      // the ScalaTsPlugin must know its version during runtime
      // -> it injects the generator artifact as a library dependency with the same version
      val out = managedResourceDirectories.in(Compile).value.head / "scala-ts.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      IO.write(props, "scala-ts properties", out)
      List(out)
    },
    crossScalaVersions := List(scala212),
    sbtPlugin := true,
    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    // pass through all jvm arguments that start with the given prefixes
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith)
    ),
    scriptedBufferLog := false,
    // adds libraryDependency to the ScalaJS sbt plugin
    // -> the ScalaTsPlugin references the ScalaJS plugin
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJsVersion),
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "test"
  )

lazy val root = project.in(file("."))
  .aggregate(generator, plugin)
  .settings (
    crossScalaVersions := Nil,
    publish / skip  := true,
  )

onChangedBuildSource in Global := ReloadOnSourceChanges