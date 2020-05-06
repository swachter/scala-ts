enablePlugins(ScalaJSPlugin)

name := "manual-dts"
scalaVersion := "2.13.1"

scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }

