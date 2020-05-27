
val scalaMetaVersion = "4.3.10"
val semanticDbVersion = "4.1.6"

lazy val commonSettings = Seq(
  organization := "eu.swdev",
  version := "0.1",
  scalaVersion := "2.12.11",
  bintrayPackageLabels := Seq("sbt","plugin"),
  bintrayVcsUrl := Some("""https://github.com/swachter/scala-ts-playground.git"""),
  bintrayOrganization := None, // TODO: what is the organization for
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
)

lazy val generator = project.in(file("generator"))
  .settings(commonSettings: _*)
  .settings(
    name := "scala-ts-generator",
    description := "library for generating TypeScript declaration files for ScalaJS sources",
    publishMavenStyle := true,
    bintrayRepository := "maven",
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.10" cross CrossVersion.full),
    scalacOptions += "-Yrangepos",
    scalacOptions += "-P:semanticdb:text:on",
    libraryDependencies += "org.scalameta" %% "semanticdb" % semanticDbVersion,
    libraryDependencies += "org.scalameta" %% "scalameta" % scalaMetaVersion,
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.2" % "test",
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(ScriptedPlugin)
  .dependsOn(generator)
  //.aggregate(generator)
  .settings(commonSettings: _*)
  .settings(
    name := s"sbt-scala-ts",
    description := "SBT plugin for generating TypeScript declaration files for ScalaJS sources",
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
