resolvers += Resolver.jcenterRepo

val defaultPluginVersion = "0.12-SNAPSHOT"

val pluginVersion = sys.props.get("plugin.version").getOrElse(defaultPluginVersion)

// the ScalaTs plugin and the ScalablyTyped plugin both transitively depend on the ScalaJS plugin
// -> set the necessary version here to overrule their version (TODO: validate that this is the correct way)
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.1")

addSbtPlugin("eu.swdev" % "sbt-scala-ts" % pluginVersion)

resolvers += Resolver.bintrayRepo("oyvindberg", "converter")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta21")
