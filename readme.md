## ScalaTsPlugin

The `ScalaTsPlugin` processes [ScalaJS](https://www.scala-js.org/) sources and generates a corresponding [TypeScript Declaration File](https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html).

### How does it work?

The implementation is based on [Scalameta](https://scalameta.org/). Information about the ScalaJS sources is collected by traversing source trees and retrieving symbol information from `SemanticDB`. Accordingly, ScalaJS sources must be compiled with the `SemanticDB` compiler plugin being added and the parameter `-Yrangepos` being set. The `ScalaTsPlugin` cares for this configuration.

The output of the plugin is a Node module of kind `ESModule`.

### Configuration

| Key | Description |
| --- | --- |
| `scalaTsOutputDir` | Directory where to put the TypeScript declaration file (default: target/node_module) |
| `scalaTsModuleName` | Name of the generated node module (default: project name) |
| `scalaTsModuleVersion` | Version of the generated node module (default: project version) |
| `scalaTsFilenamePrefix` | Filename prefix of generated JavaScript and TypeScript declaration file (default: project name) |
| `scalaTsDialect` | Dialect of the ScalaJS sources (default: Scala213) |
| `scalaTsGenerateDeclarationFile` | Generate TypeScript declaration file |
| `scalaTsGeneratePackageFile` | Generate package.json file |
| `scalaTsPackage` | Package all - generate the node module |


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
| `js.UndefOr[X]` | `p?: X` or `X `<code>&#124;</code>` undefined` depending on position |
| `js.Array[X]` | `X[]` |

Scala types that are referenced in exported definitions (i.e. vals, vars, or methods) but are not exported themselves are called _opaque_ types. In order to keep type safety, for each opaque type a corresponding marker interface is exported. Each marker interface contains a property with a name that is equal to the fully qualified type name and the value `never`. This simulates some kind of _nominal_ typing for these types instead of _structural_ typing. In order to avoid name clashes, interfaces of opaque types are included in namespaces that match their package structure. Example:

| Referenced Type | TypeScript Declaration |
| --- | --- |
| `scala.Option[X]` | `namespace scala { interface Option<X> { 'scala.Option': never } }`
 

### Translation rules

General rules:
- Type ascriptions can be omitted from ScalaJS sources; they are automatically inferred and included in the generated TypeScript declaration file.
- Multiple argument lists are flattened into a single argument list.
- Generics are supported; variance annotations are ignored.

Translation rules for top-level definitions (names given in `@JSExportTopLevel` and `@JSExport` annotations are respected):

| Scala Definition | TypeScript Definition |
| --- | --- |
| `val x: tpe` | `const x: tpe` | 
| `var x: tpe` | `let x: tpe` |
| `def x(...): tpe` | `function x(...): tpe` |
| `class X { ...member... }` | `class X { ...member... }`<br>`interface X extends ... {}` if the class extends interfaces |
| `object X { ...member... }` | `interface X { ...member... }`<br>`const X: X` | 


Translation rules for class and object members (constructor `val`/`var` parameters are also considered):

| Scala Definition | TypeScript Definition |
| --- | --- |
| `val x: tpe` | `readonly x: tpe` | 
| `var x:tpe` | `x: tpe` |
| `def x: tpe` | `get x(): tpe` |
| `def x_=(v: tpe)` | `set x(v: tpe)` |
| `def x(...): tpe` | `x(...): tpe` |

For each sealed trait a union type is created that contains all direct subtypes (classes or traits) as alternatives. The name of the union type is equal to the name of the sealed trait with a $ sign appended.

| Scala Definition | TypeScript Definition |
| --- | --- |
| `sealed trait T` | `type T$ = Case1 `<code>&#124;</code>` Case2 `<code>&#124;</code>` ...`

If the sealed trait belongs to package then the union type is defined in the corresponding namespace. Sealed trait hierarchies and generics are supported.

### Discriminated Union Types

If all union cases have a common `dicrimantor` property that has a literal type then _Flow Typing_ can be used for exhaustiveness checks.

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

function match(t: T$): number | string {
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

In addition to the interfaces that were generated for _opaque_ types, interfaces are generated for all traits in the processed sources that are base types of exported classes or objects. Finally, interfaces are generated for all types 'between' these interfaces and all exported classes and objects. The generated interfaces form an inheritance hierarchy, thereby supporting polymorphism.

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
