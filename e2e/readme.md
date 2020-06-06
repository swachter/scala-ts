### End-2-End Tests for the ScalaTsPlugin

Node must be installed in order to run the e2e tests.

The e2e tests can be run manually by invoking SBT or by the `ScripedPlugin`. In order to be run by the `ScriptedPlugin` there is a symbolic link linking this folder into the `scala-ts/src/sbt-test/scala-ts` folder.

When run manually, the version of the `ScalaTsPlugin` must be specified on the commandline. E.g.:

```
> sbt -Dplugin.version=0.2-SNAPSHOT
```

The e2e tests require that some node modules are installed. The `npmReinstallAndTest` task installs the required node modules and runs the tests:

```
sbt> npmReinstallAndTest
```

Later on, the e2e tests can directly be run by:

```
sbt> test
```
