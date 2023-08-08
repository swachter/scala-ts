### Test projects that use the ScalaTsPlugin

Note: Node must be installed in order to test these projects.

These projects are stand-alone projects that can be opened in an IDE and worked upon. In addition, each project contains a top-level `test` script file that configures a sequence of steps that is executed by the `scripted` task of the main build (cf. [Testing-sbt-plugins](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html)).

The plugin configuration of the `ScalaTsPlugin` in these projects is slightly complicated. The version number of the `ScalaTsPlugin` to use should match the version number of the current source state. The plugin configuration of the `ScalaTsPlugin` in `project/plugins.sbt` must distinguish two cases:
* The project is tested by the `scripted` task: In this case, project files are copied into a temporary folder and no version can be derived from the current source state. The version number to use is specified by the `plugin.version` system property that is specified when running the `scripted` task.
* The project is worked upon manually: In this case, the plugin version number is derived from the current source state. The derivation is done with the help of the [dynver](https://github.com/sbt/sbt-dynver) plugin. This plugin must be activated in `project/project/plugin.sbt` files in order to have the git version information available when configuring the `ScalaTsPlugin`.

Projects that are cross built for different Scala versions output their generated node module in the cross target folders. Tests written in TypeScript use [NPM workspaces](https://docs.npmjs.com/cli/v7/using-npm/workspaces) corresponding to these Scala versions. Test specification code is shared between workspaces (using symlinks) and access to corresponding generated node module is given by a relative import of the `../mod/scala-ts-mod`. 