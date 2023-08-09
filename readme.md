# ScalaTsPlugin

The `ScalaTsPlugin` processes [ScalaJS](https://www.scala-js.org/) sources, generates a corresponding [TypeScript Declaration File](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html), and bundles its output and the output of ScalaJS to form a Node module.

Built ontop of that functionality an adapter can be provided that allows to access ScalaJS code while converting between specific interoperability types and certain standard Scala types.

### Usage

Add the following lines to `project/plugins.sbt`

```
addSbtPlugin("io.github.swachter" % "sbt-scala-ts" % "0.13.0")
```

and enable the `ScalaTsPlugin` in `build.sbt`

```
enablePlugins(ScalaTsPlugin)
```

If no adapter code should be generated then no further configuration is required. In particular, no additional annotations or other kind of information must be provided.

If adapter code should be generated then additional configuration and some source annotations are required.

### How does it work?

The implementation is based on [Scalameta](https://scalameta.org/). Information about ScalaJS sources is collected by traversing source trees and retrieving symbol information from `SemanticDB`. Accordingly, ScalaJS sources must be compiled with the `SemanticDB` compiler plugin being activated. In addition, the `ScalaJSPlugin` must be configured to emit an ECMAScript module. The `ScalaTsPlugin` takes care for all this configuration.

Side note: `ScalaJS` export annotations (e.g. `@JSExportTopLevel`) are available at compile time only. SemanticDB files do not include annotation values either. Therefore, the `ScalaTsPlugin` needs to process sources in order to access export information. The SemanticDB compiler plugin option `-P:semanticdb:text:on` is used to include sources in SemanticDB files. In addition, the Scala compiler option `-Yrangepos` must be set to allow matching source file locations with symbol information from SemanticDB.

The adapter is realized by source code generation of a file containing a Scala object. This adapter object in turn, is processed by the `ScalaTsPlugin` like all other sources yielding corresponding TypeScript declarations. The adapter uses implicitly derived converters that convert between interoperability types and standard Scala types in both directions.

### SBT settings and tasks

#### Main settings and tasks

| Key | Description |
| --- | --- |
| `scalaTsModuleName` | Setting: Name of the generated node module (default: project name) |
| `scalaTsModuleVersion` | Setting: Function that transforms the project version into a node module version (default: identity function that checks if the result is a valid [semantic version](https://docs.npmjs.com/about-semantic-versioning)) |
| `scalaTsFastOpt` | Task: Generate node module including typescript declaration file based on the fastOptJS output |
| `scalaTsFullOpt` | Task: Generate node module including typescript declaration file based on the fullOptJS output |
| `scalaTsAdapterEnabled` | Setting: Determines if adapter code is generated (default: false) |

#### Dependencies

In its default configuration the `ScalaTsPlugin` considers only the sources of the project where it is enabled. If library dependencies should also be considered then the setting `scalaTsConsiderFullCompileClassPath` must be set to `true`. In that case all SemanticDB information available on the compile classpath can be used. Parts of the compile class path can be filtered by specifying regular expressions for the `scalaTsInclude` and `scalaTsExclude` settings. In order for a class path part to be considered it must match the regular expression for inclusion and not match the regular expression for exclusion. The relevant settings are:

| Key | Description |
| --- | --- |
| `scalaTsConsiderFullCompileClassPath` | Determines if the full compile class path or only the classes of the current project are considered (default: false) |
| `scalaTsInclude` | RegEx that filters entries from the full compile class path (default: `.`) |
| `scalaTsExclude` | RegEx that filters entries from the full compile class path (default: `(?!.).`) |

A common situation for a library dependency that must be considered is a cross project (named `shared`) with shared sources for client and server. For the `shared.js` project the generation of `SemanticDB` information must be activated. This can be done by invoking `.jsSettings(ScalaTsPlugin.crossProject.jsSettings)` on the cross project. A dedicated `client` project depends on the `shared.js` project and has the `ScalaTsPlugin` enabled. The [example/angular](example/angular) folder shows this setup.

#### Target directory    

The output directory of the generated node module can be configured by the `crossTarget` setting for the `fastOptJS` or `fullOptJS` tasks, respectively. E.g.:

```
(crossTarget in fastOptJS) := (baseDirectory in Compile).value / "target" / "node_module"
(crossTarget in fullOptJS) := (baseDirectory in Compile).value / "target" / "node_module"
```

#### SemanticDB version

The `ScalaTsPlugin` uses SBT's built-in [SemanticDB support](https://www.scala-sbt.org/1.x/docs/sbt-1.3-Release-Notes.html#SemanticDB+support) for generating the required SemanticDB files. The `ScalaTsPlugin` uses a default SemanticDB version. In case that the corresponding `semanticdb-scalac` compiler plugin is not available for the used Scala version, an available SemanticDB version must be configured. The `complete` command of the [Coursier](https://github.com/coursier/coursier) commandline client can be used to determine available versions. The following example shows the available versions for Scala 2.13.3 at the time of this writing:

```
> cs complete org.scalameta:semanticdb-scalac_2.13.3:
...
4.3.19
4.3.20
```

To use version `4.3.20` the following setting must be included:

```
semanticdbVersion := "4.3.20"
```

#### Type shadowing

| Key | Description |
| --- | --- |
| `scalaTsPreventTypeShadowing` | see description below (default: false) | 

TypeScript allows nested namespaces. Consider the following situation:

```
namespace x {
  interface I {}                    // interface x.I
}
namespace ns {
  namespace x {
    interface I {}                  // interface ns.x.I
    declare function f(): I         // resolves to ns.x.I
    declare function g(): x.I       // resolves to ns.x.I
    declare function h(): _root_x.I // resolves to x.I
  }
  interface X {}
}
import _root_x = x
```

Without additional measures, it is not possible to access the interface `x.I` from declarations inside the namespace `ns.x`. The reason is the existence of the interface `ns.x.I` which shadows the interface `x.I`.
 
TypeScript does not offer a 'root' namespace indicator comparable to the `_root_` package indicator available in Scala. However, using namespace imports it is possible to define namespace aliases for all top-level packages as seen above.

If the setting `scalaTsPreventTypeShadowing` is enabled then namespace aliases are emitted for all top-level packages. Types are referenced using these aliases.

## TypeScript declaration file generation
### Type correspondences

ScalaJS types are mapped into corresponding TypeScript types. ScalaJS defines type correspondences for all primitive types and provides a couple of interoperability and facade types (cf. [types](https://www.scala-js.org/doc/interoperability/types.html)). Additional facade types can be defined based on native symbols from global scope or from imported libraries. Finally, all other types are considered opaque types and are mapped into marker interfaces.

#### Primitive and literal types

| ScalaJS Type | TypeScript Type |
| --- | --- |
| `String` | `string` |
| `Boolean` | `boolean` |
| `Byte`, `Double`, `Float`, `Int`, `Short` | `number` |
| `literal type` | `literal type` |

#### Interoperability types

| ScalaJS Type | TypeScript Type |
| --- | --- |
| `js.Any` | `any` |
| `js.Object` | `object` |
| `js.UndefOr[X]` | `p?: X` or `X `<code>&#124;</code>` undefined` depending on position |
| `js.Array[X]` | `X[]` |
| `js.FunctionN[T1, ... TN, R]` | `(p1: T1, ... pn: TN) => R` |
| `js.ThisFunctionN[T0, T1, ... TN, R]` | `(this: T0, p1: T1, ... pn: TN) => R` |
| `js.TupleN[T1, ... TN]` | `[T1, ... TN]` |
| <code>js.&#124;[T1, T2]</code> | `T1 `<code>&#124;</code>` T2` |
| `js.Dictionary[T]` | `{ [key: string]: T }` |
| `js.Iterable[T]` | `Iterable<T>` |
| `js.Iterator[T]` | `Iterator<T>` |
| `js.Thenable[T]` | `PromiseLike<T>` |
|  `eu.swdev.scala.ts.tpe.ReadOnlyArray[X]` | `ReadonlyArray<X>` |

Note: The type `eu.swdev.scala.ts.tpe.ReadOnlyArray` is a type alias for `js.Array` that is part of the [adapter runtime library](#runtime-library) and that is treated differently during TypeScript declaration file generation.
  
#### Facade types

[Facade types](https://www.scala-js.org/doc/interoperability/facade-types.html) are based on underlying native JavaScript types. Facade types can be defined based on global symbols or by importing native JavaScript libraries. Facade types are mapped into the corresponding global or imported native symbol. This supports seamless interoperability for native types. In case of imported types a corresponding `import` statement is included in the generated declaration file. Default imports are supported.
 
ScalaJS includes the following facade types based on global symbols:
 
| ScalaJS Type | TypeScript Type |
| --- | --- |
| `js.Promise[T]` | `Promise<T>` |
| `js.Date` | `Date` |
| `js.RegExp` | `RegExp` |
| `js.Symbol` | `symbol` |
| `js.BigInt` | `bigint` |

Examples for custom facade types that reference global or imported symbols:

| ScalaJS Type | TypeScript Declaration |
| --- | --- |
| `@JSGlobal`<br>`class WeakMap[K <: js.Object, V]` | `WeakMap<K extends object, V>` |
| `@JSImport('module', 'ImportedType')`<br>`class SomeClass` | `import * as $module from 'module'`<br>`...`<br>`$module.ImportedType` |
| `@JSImport('module', JSImport.Default)`<br>`class SomeClass` | `import $module_ from 'module'`<br>`...`<br>`$module_` |
| `@JSImport('mod', JSImport.Namespace)` <br>`object o extends js.Object { class C }` | `import * as $mod from 'mod'`<br>`...`<br>`$mod.C` |

#### Opaque types

All other ScalaJS types are called _opaque_ types because their structure is not exposed to JavaScript. In order to preserve typesafety for opaque types a marker interface is exported for each opaque type that is referenced in an exported definition. Exported definitions reference these marker interfaces.
 
Each marker interface contains a property with a name that is equal to the fully qualified type name and the value `never`. This simulates some kind of _nominal_ typing for these types instead of _structural_ typing. In order to avoid name clashes, interfaces of opaque types are included in namespaces that match their package structure. Example:

| ScalaJS Type | TypeScript Declaration |
| --- | --- |
| `scala.Option[X]` | `namespace scala { interface Option<X> { 'scala.Option': never } }` |
 
### Translation rules

General rules:
- Type ascriptions can be omitted from ScalaJS sources; they are automatically inferred and included in the generated TypeScript declaration file.
- Multiple argument lists are flattened into a single argument list.
- Generics including upper bounds are supported.

Translation rules for top-level definitions (definitions must be annotated according to ScalaJS rules; names given in `@JSExportTopLevel`, `@JSExport`, and `@JSExportStatic` annotations are respected):

| Scala Definition | TypeScript Definition | Note |
| --- | --- | --- |
| `val x: tpe` | `const x: tpe` | 
| `var x: tpe` | `let x: tpe` |
| `def x(...): tpe` | `function x(...): tpe` |
| `class X { ...member... }` | `class X { ...member... }`<br>`interface X extends ... {}` | interface generated iff the class extends exported interfaces |
| `object X { ...member... }` | `interface X$ { ...member... }`<br>`const X: X$` |
| `trait X { ...member... }` | `interface X { ...member... }` | generated iff the trait is referenced in the exported API |
| `class X`<br>`object X { ...static... }` | `class X { ...static... }` | definitions in companion objects that are annotated by `@JSExportStatic` are exported as static class members  |
| `type X = ...` | `type X = ...` | generated iff the type alias is referenced in the exported API | 


Translation rules for class and object members (constructor `val`/`var` parameters are also considered):

| Scala Definition | TypeScript Definition | Note |
| --- | --- | --- |
| `val x: tpe` | `readonly x: tpe` | |
| `var x:tpe` | `x: tpe` | |
| `def x: tpe` | `readonly x: tpe` | if the `def` is an interface member and there is no corresponding setter |
| `def x: tpe` | `get x(): tpe` | if the `def` is a class member and there is no corresponding getter |
| `def x_=(v: tpe)` | `set x(v: tpe)` |  |
| `def x: tpe` <br> `def x_=(v: tpe)` | `x: tpe` | a corresponding getter/setter pair |
| `def x(...): tpe` | `x(...): tpe` | |

Note: Setters are not supported in TypeScript interfaces. Therefore a corresponding getter must be supplied if the setter would be part of an interface.

If a member is annotated by `@JSExport` or `@JSName` and the given name is not a valid identifier then the member is defined using so called bracket notation. Bracket notation is also used if the member name is given by a symbol. For example:

| Scala Definition | TypeScript Definition |
| --- | --- |
| `@JSName('???')`<br>`val x: tpe` | `readonly ['???']: tpe` |
| `@JSName(js.Symbol.iterator)`<br>`def iter(): js.Iterator[X] = ???` | `[Symbol.iterator](): Iterator<X>` |


Translation rule for repeated method parameters (varargs):

| Scala Method Parameter | TypeScript Method Parameter |
| --- | --- |
| `x: X*` | `...x: X[]` |

For each sealed trait that is referenced in the exported API a union type is created that contains all direct subtypes (classes, objects, or traits) as alternatives. The name of the union type is equal to the name of the sealed trait with '$u' appended.

| Scala Definition | TypeScript Definition |
| --- | --- |
| `sealed trait T` | `type T$u = Case1 `<code>&#124;</code>` Case2 `<code>&#124;</code>` ...` |

If the sealed trait belongs to a package then the union type is defined in the corresponding namespace. Sealed trait hierarchies and generics are supported.

Note that the type parameters of a union type must not match the type parameters of the corresponding sealed trait. They are derived from the type parameters of their member types. For example a sealed trait may be generic whereas all its member types are not. Member types can pass-through a type parameter to their base trait or can introduce additional type parameters. Examples are:

| Scala Definition | TypeScript Definition |
| --- | --- |
| `sealed trait Base[X]`<br>`class Case1 extends Base[String]`<br>`class Case2 extends Base[Int]` | `type Base$u = Case1 `<code>&#124;</code>` Case2` |
| `sealed trait Base[X]`<br>`class Case1[X] extends Base[X]`<br>`class Case2[X] extends Base[X]` | `type Base$u<X> = Case1<X> `<code>&#124;</code>` Case2<X>` |
| `sealed trait Base[X]`<br>`class Case1[X] extends Base[X]`<br>`class Case2[Y] extends Base[String]` | `type Base$u<X, $M1_Y> = Case1<X> `<code>&#124;</code>` Case2<$M1_Y>` |

### Discriminated Union Types

If all union cases have a common _discrimantor_ property that has a literal type then _Flow Typing_ can be used for exhaustiveness checks. (Note: `instanceof` checks are currently not supported for case classes; cf. [ScalaJS/4062](https://github.com/scala-js/scala-js/issues/4062).)

Example Scala code (without `JSExport` annotations):

```
sealed trait T
case class Case1(i: Int) { val tpe: "i" = "i" }
case class Case2(s: String) { val tpe: "s" = "s" }
```

TypeScript code:

```
function assertNever(n: never): never {
    throw new Error('never case reached')
}

function match(t: T$u): number | string {
  if (t.tpe === 'i') {
    return t.i // TypeScript knows that t has type Case1 -> access of t.i possible
  } else if (t.tpe === 's') {
    return t.s // TypeScript knows that t has type Case2 -> access of t.s possible
  } else {
    return assertNever(t) // TypeScript nows that t has type never
  }
}
```

### Generated Interface Hierarchy

In addition to the interfaces that are generated for _opaque_ types, interfaces are generated for all traits in the processed sources that are base types of exported classes or objects. Finally, interfaces are generated for all types 'between' these interfaces and all exported classes and objects. The generated interfaces form an inheritance hierarchy, thereby supporting polymorphism.

## Adapter Code Generation

The `ScalaTsPlugin` generates adapter code **only for the dependencies** of a project for which the `ScalaTsPlugin` is enabled. The reason is that adapter code generation needs the compilation result of the classes that should be adapted. The compilation result however, is not available at the source code generation stage of a project.

Considering only the dependencies of a project for adapter code generation seems to be a big disadvantage at first. However, the adapter code generation offers its greatest benefit in project setups with code that is shared between backend and frontend. ScalaJS projects without shared code can directly use the shipped [ScalaJS interoperability types](https://www.scala-js.org/doc/interoperability/types.html) like `js.UndefOr` and the accompanying implicit conversions. In projects with shared code however, the shared code can not use the interoperability types because they are not available on the backend. Therefore, adapter code is needed to convert between standard Scala types and interoperability types.

### Configuration

Adapter code generation is an opt-in feature:
 
First, classes, objects, vals, vars, and defs must be annotated if adapter code should be generated for the corresponding entities. The annotations are:

| Annotation | Description |
| --- | --- |
| `@Adapt` | can be applied on defs, vals, vars, and constructor parameters (that are exposed as vals or vars); triggers code generation to access the annotated entity |
| `@Adapt('interop type')` | allows to specify the exposed interoperability type; must be a valid Scala type (e.g. 'js.Array[Double]'; the `js` package is in scope) |
| `@AdaptConstructor` | can be applied on classes; adapter code for invoking the class constructor is generated |
| `@AdaptMembers` | can be applied on classes and objects; adapter code for all members (defs, vals, and vars) is generated |
| `@AdaptAll` | is the same as `@AdaptConstructor` and `@AdaptMembers` |

Second, adapter code generation must be enabled. Settings for adapter code generation are:

| Key | Description |
| --- | --- |
| `scalaTsAdapterEnabled` | Determines if adapter code is generated (default: false) |
| `scalaTsAdapterName` | Determines the name of the adapter object and its top-level export name (default: Adapter) | 

The setting `scalaTsAdapterEnabled := true` implies:

```
scalaTsConsiderFullCompileClassPath := true // automatically implied
scalaTsPreventTypeShadowing := true         // automatically implied 
```
#### Configuration of cross projects

The `ScalaTsPlugin` provides constants that ease the setup of cross projects. In the following example project setup the sources of the `shared` cross project may use adapter annotations. The `frontend` project that depends on the shared sources enables adapter generation: 

```
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .settings(
    // add JCenter repo + annotations dependency
    ScalaTsPlugin.crossProject.settings
  ).jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
  ).jsSettings(
    // adds semantic db settings
    ScalaTsPlugin.crossProject.jsSettings
  )

lazy val frontend = project.enablePlugins(ScalaTsPlugin).dependsOn(shared.js).settings(
  scalaTsAdapterEnabled := true,
)
```
 
### Dependencies

Adapter code generation involves two dependencies: A library that contains annotations for controlling adapter code generation and a runtime library that provides converters.

#### Annotation Library

The annotations library has the module identifier `"eu.swdev" %%% "scala-ts-annotations" % version`. The annotations library should be added to the "provided" scope because it is not required at runtime. Cross projects can add it to their common settings because the library is available both, for the `JSPlatform` and for the `JVMPlatform`. The settings constant `ScalaTsPlugin.crossProject.settings` contains the necessary settings.

#### Runtime Library

The runtime library is available for the `JSPlatform` only.  Its module identifier is: `"eu.swdev" %%% "scala-ts-runtime" % version`. This dependency is added by the `ScalaTsPlugin` automatically if adapter code generation is enabled.

### Generated Adapter Code

The adapter code contains three kinds of adapters:

1. Class adapters: they allow to create instances of ScalaJS classes and instance adapters
1. Instance adapters: they allow to access defs, vals, and vars of instances
1. Object adapters: they allow to access defs, vals, and vars of objects and package objects

#### Overall structure of adapter code

A single top level `js.Object` called adapter object is exported. It gives access to all adapted entities. It contains nested objects for all packages and object adapters.
 
For example if the package `eu.swdev` and the object `eu.Util` contain adapted entities then the following adapter structure results:

```
@JSExportTopLevel("Adapter")
object Adapter extends js.Object {
  object eu extends js.Object { // nested package
    object swdev extends js.Object { // nested package
      ...
    }
    object Util extends js.Object { // object adapter
      ...
    }
  }
}
```

These objects give access to class adapters and adapted defs, vals, and vars of objects and package objects. 
    
#### Class Adapters

Class adapters are represented by Scala objects. They are named like the class they are adapting. They have the following structure (assuming the class `x.y.SomeClass` is adapted):

```
object SomeClass {
  // create an instance; generated iff @AdaptConstructor is present
  def newDelegate(...): _root_x.y.SomeClass                 
  // create an adapter wrapping the given delegate
  def newAdapter(delegate: _root_x.y.SomeClass): SomeClass  
}
```
(Note the usage of the `_root_x` namespace identifier that is the result of the 'prevent type shadowing feature' explained above.)

The two methods `newDelegate` and `newAdapter` can conveniently be combined on the TypeScript side. A function that constructs an instance adapter given a class adapter and the necessary constructor parameters can be implemented by:

```
function newAdapter<ARGS extends any[], DELEGATE, ADAPTER>(
    classAdapter: { newDelegate: (...args: ARGS) => DELEGATE, newAdapter: (d: DELEGATE) => ADAPTER },
    ...args: ARGS
  ): ADAPTER {
    return classAdapter.newAdapter(classAdapter.newDelegate(...args))
  }
```

#### Instance Adapters

Instance adapters are represented by Scala traits. They allow to access the defs, vals, and vars of their underlying delegates. The underlying delegate of an instance adapter can be accessed by its `$delegate` property. Translatation rules are:

| Accessed ScalaJS | Adapter Code |
| --- | --- |
| `def m(p1: S1, ...): SR` | `def m(p1: I1, ...): IR = $delegate.method(p1.$cnv[S1], ...).$cnv[IR]` |
| `val value: S` | `def value: I = $delegate.value.$cnv[I]` |
| `var value: S` | `def value: I = $delegate.value.$cnv[I]`<br>`def value_=(v: I) = $delegate.value = v.$cnv[S]` |

In the translation rules above type parameters starting with `S` denote arbitrary Scala types whereas type parameter starting with `I` denote interoperability types.
 
The `$cnv` method takes care of the necessary conversion. Note that conversions may happen recursively, e.g. a Scala `List[Option[Int]]` corresponds to the interoperability type `js.Iterable[js.UndefOr[Int]]`.

#### Object Adapters

The translation rules for object adapters are nearly the same as for instance adapters. The only difference is that there is no underlying delegate. Instead, defs, vals, and vars of objects and package objects are accessed.

#### Supported Conversions

| Scala/Java Type | Interop Type<br>(default) | Additional Interop Types<br>(specified by `@Adapt`) |
| --- | --- | --- |
| `Array` | `eu.swdev.scala.ts.tpe.ReadOnlyArray` | `js.Array` |
| `List` | `js.Iterable` | `eu.swdev.scala.ts.tpe.ReadOnlyArray`, `js.Array` |
| `Option` | `js.UndefOr` | |
| `Future` | `js.Promise` | |
| `java.util.Date` | `js.Date` | `Double` |
| `FunctionX` | `js.FunctionX` | |
| `TupleX` | `js.TupleX` | |

Note that modifications of adapted arrays are not reflected in the underlying Scala array. Therefore, the default interop type for an array is `eu.swdev.scala.ts.tpe.ReadOnlyArray`. That type corresponds to TypeScript's `ReadonlyArray`.

#### Support of Inner Classes 

Instance adapters may also contain class adapters of inner classes. These class adapters can be used like a class adapters of non-inner classes. Their `newDelegate` method creates an instance of the inner class and their `newAdapter` method returns an instance adapter for an instance of an inner class.

## Appendix

### Folder Contents

- `explore`: a `ScalaJS` project for exploration
- `generator`: a library module that contains the code generator
- `runtime`: a ScalaJs library module that is used at runtime by generated adapters 
- `sbt-scala-ts`: contains the `ScalaTsPlugin`
- `sbt-scala-ts/src/sbt-test`: standalone SBT projects ([readme](sbt-scala-ts/src/sbt-test/readme.md)); some are end-2-end others test basic functionality 

### Articles

- [Writing Angular Services in Scala](https://medium.com/@antoine.doeraene/writing-angular-services-in-scala-e83fd308b7c3)

### Previous work on creating TypeScript Declaration Files

Talks:

1. [ScalaJS and Typescript: an unlikely romance](https://www.youtube.com/watch?v=KTiU6SglU4s)

Projects:

1. [scala-ts/scala-ts](https://github.com/scala-ts/scala-ts);  based on `scala.reflect`
1. [code-star/scala-tsi](https://github.com/code-star/scala-tsi); based on macros and implicit machinery
1. [waveinch/sbt-scalajs-ts-export](https://github.com/waveinch/sbt-scalajs-ts-export); based on Scala Meta trees
1. [davegurnell/bridges](https://github.com/davegurnell/bridges); based on `shapeless`
1. [sherpal/scala-ts](https://github.com/sherpal/scala-ts); based on `semanticdb`

### Acknowledgements:

Thanks to Sébastien Doeraene and Ólafur Páll Geirsson for their work and support regarding `ScalaJS` and `Scalameta`.
