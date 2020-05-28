Playground for evaluating and improving ScalaJS <-> TypeScript interoperability
===

Calling JavaScript code within ScalaJS code is a well covered topic. Here, I mainly want to explore the other direction, i.e. calling ScalaJS code from TypeScript.

There are two main issues to explore:

1. Generate  [TypeScript Declaration Files](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html) for exported ScalaJS definitions (classes, objects, methods, values).
1. Automatic conversion of types, i.e. `Option` <-> `UndefOr`, ...


Previous work on creating TypeScript Declaration Files
---

Talks:

1. [ScalaJS and Typescript: an unlikely romance](https://www.youtube.com/watch?v=KTiU6SglU4s)

Projects:

1. [scala-ts/scala-ts](https://github.com/scala-ts/scala-ts);  based on `scala.reflect`; uses IR (`ScalaModel`, `TypeScriptModel`); methods are not considered; last release 2017;
1. [code-star/scala-tsi](https://github.com/code-star/scala-tsi); based on macros and implicit machinery; generates a Scala class first, that in turn generates the TypeScript declaration file; 
1. [waveinch/sbt-scalajs-ts-export](https://github.com/waveinch/sbt-scalajs-ts-export); based on Scala Meta trees; direct, but basic mapping;
1. [davegurnell/bridges](https://github.com/davegurnell/bridges); based on `shapeless`; main focus: ADTs; methods are not considered; uses IR;
1. [sherpal/scala-ts](https://github.com/sherpal/scala-ts); based on `semanticdb`; experimental state
