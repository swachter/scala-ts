import sbt.Defaults.sbtPluginExtra

// the ScalaTs plugin and the ScalablyTyped plugin both transitively depend on the ScalaJS plugin
// -> set the necessary version here to overrule their version (TODO: validate that this is the correct way)
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.1")

// complicated way of adding the scala-ts plugin
// -> the pluginVersion is derived from the git based version using the dynver plugin
// -> the logic corresponds to: addSbtPlugin("io.github.swachter" % "sbt-scala-ts" % pluginVersion)
libraryDependencies += {
  val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
  val scalaV = (update / scalaBinaryVersion).value
  val Version = """(\d+(?:\.\d+)*).*""".r
  val pluginVersion = sys.props.get("plugin.version").getOrElse(version.value) match {
    case Version(v) if isSnapshot.value => s"$v-SNAPSHOT"
    case v => v
  }
  val dependency = "io.github.swachter" % "sbt-scala-ts" % pluginVersion
  sbtPluginExtra(dependency, sbtV, scalaV)
}

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta42")
