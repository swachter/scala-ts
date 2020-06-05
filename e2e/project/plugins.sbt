resolvers += Resolver.jcenterRepo

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("eu.swdev" % """sbt-scala-ts""" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using -Dplugin.version=... on the command line or in the scriptedLaunchOpts.""".stripMargin)
}
