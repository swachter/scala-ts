import scala.sys.process.Process

val scalaMetaVersion = "4.3.10"

lazy val scala212 = "2.12.11"
lazy val scala213 = "2.13.2"
lazy val supportedScalaVersions = List(scala212, scala213)

lazy val commonSettings = Seq(
  organization := "eu.swdev",
  version := "0.3-SNAPSHOT",
  bintrayPackageLabels := Seq("sbt","plugin"),
  bintrayVcsUrl := Some("""https://github.com/swachter/scala-ts.git"""),
  bintrayOrganization := None, // TODO: what is the organization for
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
)

lazy val generator = project.in(file("generator"))
  .settings(commonSettings: _*)
  .settings(
    name := "scala-ts-generator",
    description := "library for generating TypeScript declaration files for ScalaJS sources",
    crossScalaVersions := supportedScalaVersions,
    publishMavenStyle := true,
    bintrayRepository := "maven",
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.10" cross CrossVersion.full),
    scalacOptions += "-Yrangepos",
    scalacOptions += "-P:semanticdb:text:on",
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

// the e2e folder contains a project that uses the ScalaTsPlugin
// -> the e2e project can not be part of this build because the ScalaTsPlugin is not available in this build
// -> the e2e-mirror project includes the sources of the e2e project via symlinks
// -> this allows to conveniently work on the e2e sources while this build is opened in an IDE
// -> cf. the readme in the e2e folder for building and running the e2e tests
lazy val e2e_mirror = project
  .in(file("e2e-mirror"))
  .settings(
    name := "e2e-mirror",
    scalaVersion := scala213,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  ).enablePlugins(ScalaJSPlugin)

lazy val plugin = project
  .in(file("sbt-scala-ts"))
  .enablePlugins(ScriptedPlugin)
  .dependsOn(generator)
  .settings(commonSettings: _*)
  .settings(
    name := s"sbt-scala-ts",
    description := "SBT plugin for generating TypeScript declaration files for ScalaJS sources",
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
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.1"),
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "test"
  )

lazy val root = project.in(file("."))
  .aggregate(generator, plugin)
  .settings (
    crossScalaVersions := Nil,
    publish / skip  := true,
  )
