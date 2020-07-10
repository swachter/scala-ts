ThisBuild / scalaVersion := "2.13.2"

val shared =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).settings(
    libraryDependencies ++= Seq(
      "org.endpoints4s" %%% "algebra" % "1.0.0",
      // optional, see explanation below
      "org.endpoints4s" %%% "json-schema-generic" % "1.0.0",
    )
  ).jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
  )

val sharedJS = shared.js
val sharedJVM = shared.jvm

val client =
  project.enablePlugins(ScalaTsPlugin).settings(
    libraryDependencies += "org.endpoints4s" %%% "xhr-client" % "1.0.0",
  ).dependsOn(sharedJS)

val server =
  project.settings(
    libraryDependencies ++= Seq(
      "org.endpoints4s" %% "akka-http-server" % "1.0.0",
    )
  ).dependsOn(sharedJVM)

lazy val root = project.in(file(".")).
  aggregate(client, server).
  settings(
    publish := {},
    publishLocal := {},
  )