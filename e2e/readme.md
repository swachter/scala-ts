### End-2-End Tests for the ScalaTsPlugin

Node must be installed in order to run the e2e tests.

The e2e tests can be run manually by executing SBT in the e2e test subfolders or by the `ScriptedPlugin` from the root project. In order to be run by the `ScriptedPlugin` there are symbolic links linking e2e subfolders into the `scala-ts/src/sbt-test/scala-ts` folder.

The version of the `ScalaTsPlugin` that is used in end-2-end tests is automatically derived based on information derived from git. The version can also be specified on the commandline by setting a system property. E.g.:

```
> sbt -Dplugin.version=0.12.0
```

The e2e tests require that some node modules are installed. The `npmReinstallAndTest` task installs the required node modules and runs the tests:

```
sbt> npmReinstallAndTest
```

Later on, the e2e tests can directly be run by:

```
sbt> test
```
