## ScalaTsPlugin

The `ScalaTsPlugin` processes [ScalaJS](https://www.scala-js.org/) sources, generates a corresponding [TypeScript Declaration File](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html), and bundles its output and the output of ScalaJS to form a Node module.

### Usage

Add the following lines to `project/plugins.sbt`

```
resolvers += Resolver.jcenterRepo
addSbtPlugin("eu.swdev" % "sbt-scala-ts" % "0.8"
```

and enable the `ScalaTsPlugin` in `build.sbt`

```
enablePlugins(ScalaTsPlugin)
```
 
No further configuration is required. In particular, no additional annotations or other kind of information must be provided.

### How does it work?

The implementation is based on [Scalameta](https://scalameta.org/). Information about the ScalaJS sources is collected by traversing source trees and retrieving symbol information from `SemanticDB`. Accordingly, ScalaJS sources must be compiled with the `SemanticDB` compiler plugin being added and the parameter `-Yrangepos` being set. The `ScalaTsPlugin` cares for this configuration.

The `ScalaTsPlugin` automatically enables the `ScalaJSPlugin` and configures it to emit an ECMAScript module.

### Configuration

| Key | Description |
| --- | --- |
| `scalaTsModuleName` | Name of the generated node module (default: project name) |
| `scalaTsModuleVersion` | Transforms the project version into the node module version (default: identity function with check);<br> **the node module version must be a valid [semantic version](https://docs.npmjs.com/about-semantic-versioning)**  |
| `scalaTsFastOpt` | Task: Generate node module including typescript declaration file based on the fastOptJS output |
| `scalaTsFullOpt` | Task: Generate node module including typescript declaration file based on the fullOptJS output |

The output directory of the generated node module can be configured by the `crossTarget` setting for the `fastOptJS` or `fullOptJS` tasks, respectively. E.g.:

```
(crossTarget in fastOptJS) := (baseDirectory in Compile).value / "target" / "node_module"
(crossTarget in fullOptJS) := (baseDirectory in Compile).value / "target" / "node_module"
```

### Type mapping

Primitive types

| Scala Type | TypeScript Type |
| --- | --- |
| `String` | `string` |
| `Boolean` | `boolean` |
| `Int`, `Double` | `number` |
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
| `js.Iterable[T]` | `Iterable<T>` |
| `js.Iterator[T]` | `Iterator<T>` |
| `js.Date` | `Date` |
| `js.RegExp` | `RegExp` |
| `js.Symbol` | `symbol` |

Scala types that are referenced in exported definitions (i.e. vals, vars, or methods) but are not exported themselves are called _opaque_ types. Three kinds of opaque types are distinguished:

1. Types that reference global types (i.e. types that are annotated with `@JSGlobal`)
1. Types that reference imported types (i.e. types that are annotated with `@JSImport`)
1. Types that do not reference a global or an imported type
 
Scala types that reference a global or an imported TypeScript type (so called facade types) are represented by the corresponding global or imported symbol. This supports seamless interoperability for TypeScript types from external libraries or from global scope. In case of imported types a corresponding `import` statement is included in the generated declaration file. Default imports are supported.

In order to keep type safety for opaque types that do not reference a global or imported type a corresponding marker interface is exported. Each marker interface contains a property with a name that is equal to the fully qualified type name and the value `never`. This simulates some kind of _nominal_ typing for these types instead of _structural_ typing. In order to avoid name clashes, interfaces of opaque types are included in namespaces that match their package structure. Example:

| Referenced Type | TypeScript Declaration |
| --- | --- |
| `@JSGlobal`<br>`class WeakMap<K <: js.Object, V>` | `WeakMap<K extends object, V>` |
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

| Scala Definition | TypeScript Definition |
| --- | --- |
| `val x: tpe` | `readonly x: tpe` | 
| `var x:tpe` | `x: tpe` |
| `def x: tpe` | `get x(): tpe` |
| `def x_=(v: tpe)` | `set x(v: tpe)` |
| `def x(...): tpe` | `x(...): tpe` |

If a member is annotated by `@JSExport` or `@JSName` and the given name is not a valid identifier then the member is defined using so called bracket notation. Bracket notation is also used if the member name is given by a symbol. For example:

| Scala Definition | TypeScript Definition |
| --- | --- |
| `@JSName('???')`<br>`val x: tpe` | `readonly ['???']: tpe` |
| `@JSName(js.Symbol.iterator)`<br>`def iter(): js.Iterator[X] = ???` | `[Symbol.iterator](): Iterator<X>` |


Translation rule for repeated method parameters (varargs):

| Scala Method Parameter | TypeScript Method Parameter |
| --- | --- |
| `x: X*` | `...x: X[]` |

For each sealed trait a union type is created that contains all direct subtypes (classes or traits) as alternatives. The name of the union type is equal to the name of the sealed trait with '$u' appended.

| Scala Definition | TypeScript Definition |
| --- | --- |
| `sealed trait T` | `type T$u = Case1 `<code>&#124;</code>` Case2 `<code>&#124;</code>` ...`

If the sealed trait belongs to a package then the union type is defined in the corresponding namespace. Sealed trait hierarchies and generics are supported.

### Discriminated Union Types

If all union cases have a common _discrimantor_ property that has a literal type then _Flow Typing_ can be used for exhaustiveness checks. (Note: `instanceof` checks are currently not supported for case classes; cf. [scala-js / 4062](https://github.com/scala-js/scala-js/issues/4062).)

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

### Folder Contents

- `e2e`: standalone SBT project with end-2-end tests ([`readme`](e2e/readme.md))
- `e2e-mirror`: contains a symbolic link to the sources of the `e2e` folder; allows to edit the sources of the `e2e` test when working with this build
- `explore`: a `ScalaJS` project for exploration
- `generator`: a library module that contains the code generator
- `sbt-scala-ts`: contains the `ScalaTsPlugin`

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

Thanks to Sébastien Doeraene and Ólafur Páll Geirsson for their work on `ScalaJS` and `Scalameta`, respectively. They helped alot by giving supportive answers.
