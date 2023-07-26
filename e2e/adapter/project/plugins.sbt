import sbt.Defaults.sbtPluginExtra

// complicated way of adding the scala-ts plugin
// -> the pluginVersion is derived from the git based version using the dynver plugin
// -> the logic corresponds to: addSbtPlugin("com.github.swachter" % "sbt-scala-ts" % pluginVersion)
libraryDependencies += {
  val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
  val scalaV = (update / scalaBinaryVersion).value
  val Version = """(\d+(?:\.\d+)*).*""".r
  val pluginVersion = sys.props.get("plugin.version").getOrElse(version.value) match {
    case Version(v) if isSnapshot.value => s"$v-SNAPSHOT"
    case v => v
  }
  val dependency = "com.github.swachter" % "sbt-scala-ts" % pluginVersion
  sbtPluginExtra(dependency, sbtV, scalaV)
}

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")