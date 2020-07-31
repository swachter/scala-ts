# ScalaTsPlugin

The `ScalaTsPlugin` processes [ScalaJS](https://www.scala-js.org/) sources, generates a corresponding [TypeScript Declaration File](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html), and bundles its output and the output of ScalaJS to form a Node module.

Built ontop of that functionality an adapter can be provided that allows to access ScalaJS code while converting between specific interoperability types and certain standard Scala types.

### Usage

Add the following lines to `project/plugins.sbt`

```
resolvers += Resolver.jcenterRepo
addSbtPlugin("eu.swdev" % "sbt-scala-ts" % "0.9")
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

The adapter is realized by source code generation. The source code of a Scala adapter object is generated. The adapter object in turn, is processed by the `ScalaTsPlugin` like all other sources to yielding corresponding TypeScript declarations.

### SBT settings and tasks

Main settings and tasks are:

| Key | Description |
| --- | --- |
| `scalaTsModuleName` | Setting: Name of the generated node module (default: project name) |
| `scalaTsModuleVersion` | Setting: Function that transforms the project version into a node module version (default: identity function that checks if the result is a valid [semantic version](https://docs.npmjs.com/about-semantic-versioning)) |
| `scalaTsFastOpt` | Task: Generate node module including typescript declaration file based on the fastOptJS output |
| `scalaTsFullOpt` | Task: Generate node module including typescript declaration file based on the fullOptJS output |
| `scalaTsAdapterEnabled` | Setting: Determines if adapter code is generated (default: false) |

In its default configuration the `ScalaTsPlugin` considers only the sources of the project where it is enabled. If library dependencies should also be considered then the setting `scalaTsConsiderFullCompileClassPath` must be set to `true`. In that case all SemanticDB information available on the compile classpath can be used. Parts of the compile class path can be filtered by specifying regular expressions for the `scalaTsInclude` and `scalaTsExclude` settings. In order for a class path part to be considered it must match the regular expression for inclusion and not match the regular expression for exclusion. The relevant settings are:

| Key | Description |
| --- | --- |
| `scalaTsConsiderFullCompileClassPath` | Determines if the full compile class path or only the classes of the current project are considered (default: false) |
| `scalaTsInclude` | RegEx that filters entries from the full compile class path (default: `.`) |
| `scalaTsExclude` | RegEx that filters entries from the full compile class path (default: `(?!.).`) |
| `scalaTsPreventTypeShadowing` | see description below (default: false) | 

A common situation for a library dependency that must be considered is a cross project (named `shared`) with shared sources for client and server. For the `shared.js` project the generation of `SemanticDB` information must be activated. This can be done by invoking `.jsSettings(ScalaTsPlugin.semanticDbSettings)` on the cross project. A dedicated `client` project depends on the `shared.js` project and has the `ScalaTsPlugin` enabled. The [example/angular](example/angular) folder shows this setup.    

The output directory of the generated node module can be configured by the `crossTarget` setting for the `fastOptJS` or `fullOptJS` tasks, respectively. E.g.:

```
(crossTarget in fastOptJS) := (baseDirectory in Compile).value / "target" / "node_module"
(crossTarget in fullOptJS) := (baseDirectory in Compile).value / "target" / "node_module"
```

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
 
TypeScript does not offer a 'root' namespace indicator comparable to the `_root_` package indicator available in Scala. However, using namespace imports it is possible to define namespace aliases for all top-level packages.

If the setting `scalaTsPreventTypeShadowing` is enabled then namespace aliases are emitted for all top-level packages. Types are referenced using these aliases.

## TypeScript declaration file generation
### Type mapping

Primitive types

| Scala Type | TypeScript Type |
| --- | --- |
| `String` | `string` |
| `Boolean` | `boolean` |
| `Byte`, `Double`, `Float`, `Int`, `Short` | `number` |
| `literal type` | `literal type` |

Supported ScalaJS Interoperability Types

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
| `js.Promise[T]` | `Promise<T>` |
| `js.Thenable[T]` | `PromiseLike<T>` |
| `js.Iterable[T]` | `Iterable<T>` |
| `js.Iterator[T]` | `Iterator<T>` |
| `js.Date` | `Date` |
| `js.RegExp` | `RegExp` |
| `js.Symbol` | `symbol` |
| `js.BigInt` | `bigint` |

Scala types that are referenced in exported definitions (i.e. classes, objects, vals, vars, or methods) but are not exported themselves are called _opaque_ types. Three kinds of opaque types are distinguished:

1. Types that reference global types (i.e. types that are annotated with `@JSGlobal`)
1. Types that reference imported types (i.e. types that are annotated with `@JSImport`)
1. Types that do not reference a global or an imported type
 
Scala types that reference a global or an imported TypeScript type (so called facade types) are represented by the corresponding global or imported symbol. This supports seamless interoperability for TypeScript types from external libraries or from global scope. In case of imported types a corresponding `import` statement is included in the generated declaration file. Default imports are supported.

In order to keep type safety for opaque types that do not reference a global or imported type a corresponding marker interface is exported. Each marker interface contains a property with a name that is equal to the fully qualified type name and the value `never`. This simulates some kind of _nominal_ typing for these types instead of _structural_ typing. In order to avoid name clashes, interfaces of opaque types are included in namespaces that match their package structure. Example:

| Referenced Type | TypeScript Declaration |
| --- | --- |
| `@JSGlobal`<br>`class WeakMap[K <: js.Object, V]` | `WeakMap<K extends object, V>` |
| `@JSImport('module', 'ImportedType')`<br>`class SomeClass` | `import * as $module from 'module'`<br>`...`<br>`$module.ImportedType` |
| `@JSImport('module', JSImport.Default)`<br>`class SomeClass` | `import $module_ from 'module'`<br>`...`<br>`$module_` |
| `@JSImport('mod', JSImport.Namespace)` <br>`object o extends js.Object { class C }` | `import * as $mod from 'mod'`<br>`...`<br>`$mod.C` |
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

| Scala Definition | TypeScript Definition | Note
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
| `sealed trait T` | `type T$u = Case1 `<code>&#124;</code>` Case2 `<code>&#124;</code>` ...`

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

The `ScalaTsPlugin` generates adapter code only for the **dependencies** of a project for which the `ScalaTsPlugin` is enabled. The reason is that adapter code generation needs the compilation result of the classes that should be adapted. The compilation result however, is not available at the source code generation stage of a project when the adapter code is generated.

Considering only the dependencies of a project for adapter code generation seems to be a big disadvantage at first. However, the adapter code generation offers its most benefit in project setups that have shared code between backend and frontend.
 
In projects that contain ScalaJS code only that code can directly use the shipped [ScalaJS interoperability types](https://www.scala-js.org/doc/interoperability/types.html) like `js.UndefOr`. In projects with shared code however, that shared code can not use the interoperability types because they are not available on the backend. Therefore adapter code is needed to convert between standard Scala types and interoperability types.

### Configuration

Adapter code generation is an opt-in feature. First, the setting

```
scalaTsAdapterEnabled := true
```

must be defined. This setting automatically implies

```
scalaTsConsiderFullCompileClassPath := true // automatically implied
scalaTsPreventTypeShadowing := true         // automatically implied 
```
 
Second, classes, objects, vals, vars, and defs must be annotated if adapter code should be generated for the corresponding entities. The annotations are:

| Annotation | Description |
| --- | --- |
| `@Adapt` | can be used with defs, vals, vars, and constructor parameters (that are exposed as vals or vars); triggers code generation to access the annotated entity |
| `@Adapt('interop type')` | allows to specify the exposed interoperability type; must be a valid Scala type (e.g. 'js.Array[Double]'; the `js` package is in scope) |
| `@AdaptConstructor` | can be used with classes; adapter code for invoking the class constructor is generated |
| `@AdaptMembers` | can be used with classes and objects; adapter code for all members (defs, vals, and vars) is generated |
| `@AdaptAll` | is the same as `@AdaptConstructor` and `@AdaptMembers` |

### Generated Adapter Code

The adapter code contains two kinds of adapters:

1. Class adapters: they allow to create instances and instance adapters
1. Instance adapters: they allow to access the defs, vals, and vars of instances

In addition, defs, vals, and vars of companion objects or package objects can also be accessed.

#### Class Adapters

Class adapters are represented by Scala objects. They are named like the class they are adapting. They have the following structure (assuming the the class `x.y.SomeClass` is adapted):

```
object SomeClass {
  // create an instance; generated iff @AdaptConstructor is present
  def newDelegate(...): _root_x.y.SomeClass                 
  // create an adapter wrapping the given delegate
  def newAdapter(delegate: _root_x.y.SomeClass): SomeClass  
}
```

On the TypeScript side the two methods can conveniently be combined, i.e. given the necessary constructor parameters returning an adapter for a newly created instance:

```
function newAdapter<ARGS extends any[], DELEGATE, ADAPTER>(
    classAdapter: { newDelegate: (...args: ARGS) => DELEGATE, newAdapter: (d: DELEGATE) => ADAPTER },
    ...args: ARGS
  ): ADAPTER {
    return classAdapter.newAdapter(classAdapter.newDelegate(...args))
  }
```

#### Instance Adapters

Instance adapters are represented by Scala traits. They allow to access the defs, vals, and vars of their underlying delegates. Access to an underlying `val` is implemented by a `def` without parameters that takes care of the necessary conversion. Access to an underlying `var` is implemented by a pair of `def`s being getter/setter of the `var`.

#### Support for Inner Classes 

Instance adapters may also contain class adapters of inner classes. Because of a current limitation of ScalaJS (cf. [ScalaJS/4142](https://github.com/scala-js/scala-js/issues/4142)) an additional field for the class adapter must be generated. The field is named like the adapted class with an `$a` suffix. The class adapter can be used to instantiate classes and adapters like normal classes.

## Appendix

### Folder Contents

- `e2e`: standalone SBT projects for end-2-end tests ([`readme`](e2e/readme.md))
- `explore`: a `ScalaJS` project for exploration
- `generator`: a library module that contains the code generator
- `sbt-scala-ts`: contains the `ScalaTsPlugin`

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
