## ScalaTsPlugin

The `ScalaTsPlugin` processes [ScalaJS](https://www.scala-js.org/) sources and generates a corresponding [TypeScript Declaration File](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html).

### Contents
 
The `scala-ts` folder contains an SBT project for building the plugin ([`readme`](scala-ts/readme.md)).

The `test` folder contains a project that uses the plugin and includes a number of unit tests ([`readme`](test/readme.md)).

### Previous work on creating TypeScript Declaration Files

Talks:

1. [ScalaJS and Typescript: an unlikely romance](https://www.youtube.com/watch?v=KTiU6SglU4s)

Projects:

1. [scala-ts/scala-ts](https://github.com/scala-ts/scala-ts);  based on `scala.reflect`; uses IR (`ScalaModel`, `TypeScriptModel`); methods are not considered; last release 2017;
1. [code-star/scala-tsi](https://github.com/code-star/scala-tsi); based on macros and implicit machinery; generates a Scala class first, that in turn generates the TypeScript declaration file; 
1. [waveinch/sbt-scalajs-ts-export](https://github.com/waveinch/sbt-scalajs-ts-export); based on Scala Meta trees; direct, but basic mapping;
1. [davegurnell/bridges](https://github.com/davegurnell/bridges); based on `shapeless`; main focus: ADTs; methods are not considered; uses IR;
1. [sherpal/scala-ts](https://github.com/sherpal/scala-ts); based on `semanticdb`; experimental state
