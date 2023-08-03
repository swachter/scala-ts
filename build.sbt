/*
  Release process:

    sbt> reload
    sbt> +test
    sbt> +publishLocal
    sbt> scripted


    - set next release version in readme.md & commit changes

    > git tag -a v<version> -m "v<version>"
    > git push origin v<version>
 */

import sbt.internal.inc.ScalaInstance

import scala.sys.process.Process

val scalaMetaVersion = "4.8.5"

// the ScalaJS version the ScalaTsPlugin depends upon
// (this build also uses the ScalaJS plugin; that version is configured in project/plugins.sbt)
val scalaJsVersion = "1.13.2"

val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala3   = "3.4.0-RC1-bin-SNAPSHOT" // "3.3.0"
//val scala3   = "3.3.0"

val scalaVersions = List(scala3, scala213, scala212)

val SnapshotVersion = """(\d+(?:\.\d+)*).*-SNAPSHOT""".r

//ThisBuild / fork := true
//ThisBuild / javaOptions ++= Seq("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")

inThisBuild(
  List(
    organization := "io.github.swachter",
    version ~= {
      case SnapshotVersion(v) => s"$v-SNAPSHOT"
      case v                  => v
    },
    versionScheme := Some("semver-spec"),
    homepage := Some(url("https://github.com/swachter/scala-ts")),
    // Alternatively License.Apache2 see https://github.com/sbt/librarymanagement/blob/develop/core/src/main/scala/sbt/librarymanagement/License.scala
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "swachter",
        "Stefan Wachter",
        "stefan.wachter@gmx.de",
        url("https://github.com/swachter")
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
  ))

lazy val annotations = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "scala-ts-annotations",
    description := "compile time only library including annotations for ScalaTs",
    crossScalaVersions := scalaVersions,
  )

lazy val runtime = project
  .dependsOn(annotations.js)
  .settings(
    name := "scala-ts-runtime",
    description := "runtime library that contains conversion logic when using adapters",
    crossScalaVersions := scalaVersions,
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.16" % "test",
    libraryDependencies += ("org.scala-js" %%% "scalajs-fake-insecure-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13),
//    scalacOptions += "-Xlog-implicits"
  )
  .enablePlugins(ScalaJSPlugin)

lazy val generator = project
  .in(file("generator"))
  .dependsOn(annotations.jvm)
  .settings(
    name := "scala-ts-generator",
    description := "library for generating TypeScript declaration files from ScalaJS sources",
    crossScalaVersions := scalaVersions,
    // activate the SemanticDB compiler plugin in the test configuration only
    // -> add the compiler plugin dependency
    // -> set autoCompilerPlugins := false -> no "-Xplugin=..." Scalac option is added automatically
    // -> add the necessary Scalac options manually in the test configuration
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => Nil
        case _            => List(compilerPlugin("org.scalameta" % "semanticdb-scalac" % scalaMetaVersion cross CrossVersion.full))
      }
    },
    autoCompilerPlugins := false,
    ivyConfigurations += Configurations.CompilerPlugin,
    Test / scalacOptions ++= Classpaths.autoPlugins(update.value, Seq(), ScalaInstance.isDotty(scalaVersion.value)),
    Test / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => List("-Ysemanticdb", "-semanticdb-text")
        case _            => List("-Yrangepos", "-P:semanticdb:text:on")
      }
    },
    libraryDependencies += ("org.scalameta" %% "scalameta"     % scalaMetaVersion).cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("org.scala-js"  %% "scalajs-stubs" % "1.1.0").cross(CrossVersion.for3Use2_13),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % "test",
  )

lazy val explore = project
  .in(file("explore"))
  .settings(
    name := "explore",
    scalaVersion := scala3,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    test := {
      (Compile / fastOptJS).value
      Process("npm" :: "t" :: Nil, baseDirectory.value, "PATH" -> System.getenv("PATH")) !
    }
  )
  .enablePlugins(ScalaJSPlugin)

lazy val plugin = project
  .in(file("sbt-scala-ts"))
  .enablePlugins(ScriptedPlugin)
  .dependsOn(generator)
  .settings(
    name := "sbt-scala-ts",
    description := "SBT plugin for generating TypeScript declaration files for ScalaJS sources",
    Compile / resourceGenerators += Def.task {
      // the ScalaTsPlugin must know its version during runtime
      // -> it injects the generator artifact as a library dependency with the same version
      val out   = (Compile / managedResourceDirectories).value.head / "scala-ts.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      IO.write(props, "scala-ts properties", out)
      List(out)
    },
    crossScalaVersions := List(scala212),
    sbtPlugin := true,
    // the scripted plugin copies the test projects into temporary folders
    // -> the git based automatic plugin version derivation for the scala-ts plugin to use does not work
    // -> specify the scala-ts plugin version by a system property
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    // pass through all jvm arguments that start with the given prefixes
//    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
//      a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith)
//    ),
    scriptedBufferLog := false,
    // clean scripted projects before they are tested
    // -> scripted projects are copied into temp folders before they are tested
    // -> cleaning includes their node_module folders
    // -> the node_modules folders may include broken symlinks that should point to node packages that are
    //    created by the ScalaTsPlugin (they originate from "file:..." entries in package.json)
    // -> such broken symlinks would make the copy step of the scripted task fail
    scriptedDependencies := {
      val scriptedDir = baseDirectory.value / "src" / "sbt-test"
      val projectDirs = IO.listFiles(scriptedDir).flatMap(IO.listFiles(_))
      projectDirs.foreach { dir =>
        val r = (Process("sbt" :: "clean" :: Nil, dir, "PATH" -> System.getenv("PATH")) !)
        if (r != 0) {
          throw new MessageOnlyException(s"Could not clean scripted project in folder: $dir")
        }
      }
    },
    // adds libraryDependency to the ScalaJS sbt plugin
    // -> the ScalaTsPlugin references the ScalaJS plugin
    addSbtPlugin("org.scala-js"       % "sbt-scalajs"       % scalaJsVersion),
    addSbtPlugin("org.portable-scala" % "sbt-platform-deps" % "1.0.0"),
    libraryDependencies += ("org.scala-js" %% "scalajs-stubs" % "1.1.0" % "test").cross(CrossVersion.for3Use2_13),
  )

lazy val root = project
  .in(file("."))
  .aggregate(annotations.jvm, annotations.js, runtime, generator, plugin)
  .settings(
    name := "scala-ts",
    crossScalaVersions := Nil,
    publish / skip := true,
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
