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

The top-level module folder contains a couple of symbolic links:

```
index.d.ts -> ../src/main/module/index.d.ts
index.js -> ../target/scala-2.13/manual-dts-fastopt.js
index.js.map -> ../target/scala-2.13/manual-dts-fastopt.js.map
package.json -> ../src/main/module/package.json
```

The module is installed by running:

```
> npm install ./module
```

Finally the TypeScript application `app.ts` can be compiled and run by:

```
> npm run start
```
