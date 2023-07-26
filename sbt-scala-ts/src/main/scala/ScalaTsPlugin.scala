package eu.swdev.scala.ts

import java.util.regex.Pattern

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.internal.util.ManagedLogger

object ScalaTsPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = ScalaJSPlugin

  object autoImport {
    val scalaTsModuleName = settingKey[String]("Name of the generated node module (default: project name)")
    val scalaTsModuleVersion =
      settingKey[String => String]("Maps the project version into a node module version (default: identity function with check)")

    val scalaTsPreventTypeShadowing = settingKey[Boolean]("Determines if type shadowing is prevented (default: false)")
    val scalaTsConsiderFullCompileClassPath = settingKey[Boolean](
      "Determines if the full compile class path or only the classes of the current project are considered (default: false)")
    val scalaTsInclude = settingKey[Pattern]("RegEx that filters entries from the full compile class path (default: .)")
    val scalaTsExclude = settingKey[Pattern]("RegEx that filters entries from the full compile class path (default: (?!.).)")

    val scalaTsFastOpt = taskKey[Unit]("Generate node module including typescript declaration file based on the fastOptJS output")
    val scalaTsFullOpt = taskKey[Unit]("Generate node module including typescript declaration file based on the fullOptJS output")

    val scalaTsAdapterEnabled = settingKey[Boolean]("Determines if adapter code is generated (default: false)")
    val scalaTsAdapterName    = settingKey[String]("Name of adapter (default: Adapter)")

    // development support settings
    val scalaTsValidate          = settingKey[Boolean]("Determines if generation results are compared given expected results")
    val scalaTsChangeForkOptions = settingKey[ForkOptions => ForkOptions]("Allows to change the fork options (default: identity function)")

    def withDebugForkOptions(port: Int)(fo: ForkOptions) =
      fo.withRunJVMOptions(Vector(s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port"))
  }

  import autoImport._

  override lazy val globalSettings = Seq(
    scalaTsPreventTypeShadowing := false,
    scalaTsModuleVersion := semanticVersionCheck,
    scalaTsConsiderFullCompileClassPath := false,
    scalaTsInclude := Pattern.compile("."),
    scalaTsExclude := Pattern.compile("(?!.)."),
    scalaTsAdapterEnabled := false,
    scalaTsAdapterName := "Adapter",
    scalaTsChangeForkOptions := identity,
    scalaTsValidate := false,
  )

  lazy val semanticDbSettings = Seq(
    semanticdbEnabled := true,
    semanticdbIncludeInJar := true,
    semanticdbVersion := "4.8.5",
    semanticdbOptions := Seq("-P:semanticdb:text:on"),
    scalacOptions += "-Yrangepos",
  )

  lazy val scalaJsSettings = Seq(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSUseMainModuleInitializer := false,
  )

  lazy val generatorDependency = "com.github.swachter" %% "scala-ts-generator" % BuildInfo.version

  // settings for cross projects
  object crossProject {

    lazy val settings = Def.settings(
      libraryDependencies += "com.github.swachter" %%% "scala-ts-annotations" % BuildInfo.version % "provided"
    )

    lazy val jsSettings = semanticDbSettings
  }

  override lazy val projectSettings: Seq[Setting[_]] = semanticDbSettings ++ scalaJsSettings ++ Seq(
    scalaTsModuleName := name.value,
    // the scala-ts generator uses scala-reflect in order to access the compiled classes of the current project
    // -> the Scala version of the scala-reflect jar must match the Scala version of the compiled classes (e.g. 2.12 or 2.13)
    // -> fork a Scala process for the scala-ts generator for the required Scala version
    // -> add the scala-ts-generator jar as a dependency to the project in order to have it on the classpath
    // -> use (classDirectory +: fullClassPath) as the classpath for the forked process
    libraryDependencies += generatorDependency,
    libraryDependencies ++= (if (scalaTsAdapterEnabled.value) Seq("com.github.swachter" %%% "scala-ts-runtime" % BuildInfo.version) else Seq()),
    Compile / sourceGenerators += Def.task {
      generateAdapter(
        scalaTsAdapterEnabled.value,
        AdapterGeneratorMain.Config(
          adapterFile = (Compile / sourceManaged).value / (scalaTsAdapterName.value + ".scala"),
          adapterName = scalaTsAdapterName.value,
          include = scalaTsInclude.value,
          exclude = scalaTsExclude.value,
          (Compile / dependencyClasspath).value.map(_.data).toList,
          scalaTsValidate.value,
        ),
        scalaTsChangeForkOptions.value,
        streams.value.log
      )
    }.taskValue,
    scalaTsFastOpt := {
      (Compile / fastOptJS).value
      forkDtsGenerator(
        DtsGeneratorMain.Config(
          (Compile / fastOptJS / artifactPath).value,
          scalaTsPreventTypeShadowing.value || scalaTsAdapterEnabled.value,
          scalaTsModuleName.value,
          scalaTsModuleVersion.value.apply(version.value),
          scalaTsConsiderFullCompileClassPath.value || scalaTsAdapterEnabled.value,
          scalaTsInclude.value,
          scalaTsExclude.value,
          (Compile / classDirectory).value,
          (Compile / fullClasspath).value.map(_.data).toList,
          scalaTsValidate.value,
        ),
        scalaTsChangeForkOptions.value,
        streams.value.log
      )
    },
    scalaTsFullOpt := {
      (Compile / fullOptJS).value
      forkDtsGenerator(
        DtsGeneratorMain.Config(
          (Compile / fullOptJS / artifactPath).value,
          scalaTsPreventTypeShadowing.value || scalaTsAdapterEnabled.value,
          scalaTsModuleName.value,
          scalaTsModuleVersion.value.apply(version.value),
          scalaTsConsiderFullCompileClassPath.value || scalaTsAdapterEnabled.value,
          scalaTsInclude.value,
          scalaTsExclude.value,
          (Compile / classDirectory).value,
          (Compile / fullClasspath).value.map(_.data).toList,
          scalaTsValidate.value,
        ),
        scalaTsChangeForkOptions.value,
        streams.value.log
      )
    }
  )

  def semanticVersionCheck(v: String): String = {
    // regex copied from https://semver.org/
    if (v matches "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$") {
      v
    } else {
      throw new MessageOnlyException(
        "version '$v' is not a valid semantic version (cf. https://semver.org/); adjust the project version or set the scalaTsModuleVersion function")
    }
  }

  def forkDtsGenerator(
      config: DtsGeneratorMain.Config,
      changeForkOptions: ForkOptions => ForkOptions,
      log: ManagedLogger
  ): Unit = {
    val options = ForkOptions()
      .withBootJars((config.compileClassDir +: config.compileFullClasspath).toVector)
      .withOutputStrategy(OutputStrategy.LoggedOutput(log))
      .withEnvVars(config.asEnvVars)
    Fork.scala(
      changeForkOptions(options),
      Seq(s"${classOf[DtsGeneratorMain].getName}")
    )
  }

  def generateAdapter(enabled: Boolean,
                      config: AdapterGeneratorMain.Config,
                      changeForkOptions: ForkOptions => ForkOptions,
                      log: ManagedLogger): Seq[File] = {
    if (enabled) {
      forkAdapterGenerator(config, changeForkOptions, log)
      Seq(config.adapterFile)
    } else {
      Seq()
    }
  }

  def forkAdapterGenerator(
      config: AdapterGeneratorMain.Config,
      changeForkOptions: ForkOptions => ForkOptions,
      log: ManagedLogger
  ): Unit = {
    val options = ForkOptions()
      .withBootJars(config.compileFullClasspath.toVector)
      .withOutputStrategy(OutputStrategy.LoggedOutput(log))
      .withEnvVars(config.asEnvVars)
    Fork.scala(
      changeForkOptions(options),
      Seq(s"${classOf[AdapterGeneratorMain].getName}")
    )
  }
}
