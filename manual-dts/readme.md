Manual experiments for TypeScript -> Scala interop
===

Scala sources are compiled into JavaScript containing an EcmaScript module. ScalaJS is configured to output an EcmaScript module by setting the corresponding `ModuleKind` in `build.sbt`:
 
```
scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
```

The EcmaScript module in turn is augmented by a `package.json` and a hand crafted [TypeScript Declaration File](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html). The module is used by the TypeScript compiler and Node to compile and run the TypeScript application `app.ts`.

Commands and Setup:
---

Scala sources are compiled into JavaScript by:

```
> sbt fastOptJS
```

The top-level `module` folder contains a couple of symbolic links:

```
index.d.ts -> ../src/main/module/index.d.ts
index.js -> ../target/scala-2.13/manual-dts-fastopt.js
index.js.map -> ../target/scala-2.13/manual-dts-fastopt.js.map
package.json -> ../src/main/module/package.json
```

This folder represents a node module that contains the transpiled ScalaJS code and its meta data. In particular, the `package.json` file specifies that the module is an EcmaScript module named 'from-scala'. (The 'from-scala' module is installed as a local module by `npm i ./module`.)

Before running the test application the necessary node modules have to be installed:

```
> npm ci
```

Finally the TypeScript application `app.ts` can be compiled and run by:

```
> npm run start
```
