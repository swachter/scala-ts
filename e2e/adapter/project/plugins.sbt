resolvers += Resolver.jcenterRepo

val defaultPluginVersion = "0.12-SNAPSHOT"

val pluginVersion = sys.props.get("plugin.version").getOrElse(defaultPluginVersion)

addSbtPlugin("eu.swdev" % "sbt-scala-ts" % pluginVersion)

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")