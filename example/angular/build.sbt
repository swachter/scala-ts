ThisBuild / scalaVersion := "2.13.3"

val shared =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).settings(
    libraryDependencies ++= Seq(
      "org.endpoints4s" %%% "algebra" % "1.0.0",
      "org.endpoints4s" %%% "json-schema-generic" % "1.0.0",
    )
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
    scalaTsModuleName := "scala-client",
    scalaTsConsiderFullCompileClassPath := true,
    libraryDependencies += "org.endpoints4s" %%% "xhr-client" % "1.0.0+sjs1",
  ).dependsOn(sharedJS)

val server =
  project.settings(
    libraryDependencies ++= Seq(
      "org.endpoints4s" %% "akka-http-server" % "1.0.0",
    )
  ).dependsOn(sharedJVM)

lazy val root = project.in(file(".")).
  aggregate(client, server, sharedJS, sharedJVM).
  settings(
    name := "scala-ts-angular",
    publish := {},
    publishLocal := {},
  )