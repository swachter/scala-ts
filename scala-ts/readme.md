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

Supported ScalaJS Interoperability Types

| ScalaJS Type | TypeScript Type |
| --- | --- |
| `js.UndefOr[X]` | `p?: X` or `X `<code>&#124;</code>` undefined` depending on position |
| `js.Array[X]` | `X[]` |

Scala types that are referenced in exported definitions (e.g. methods) but are not exported themselves are called _opaque_ types. In order to keep type safety, for each opaque type a corresponding marker interface is exported. Each marker interface contains a property with a name that is equal to the fully qualified type name and the value `never`. This simulates some kind of _nominal_ typing for these types instead of _structural_ typing. In order to avoid name clashes, interfaces of opaque types are included in namespaces that match their package structure. Example:

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
| `def x(...): tpe` | `x(...): tpe` |
| `class X { ...member... }` | `class X { ...member... }` |
| `object X { ...member... }` | `const X { ...member... }` | 


Translation rules for class and object members (constructor `val`/`var` parameters are also considered):

| Scala Definition | TypeScript Definition |
| --- | --- |
| `val x: tpe` | `readonly x: tpe` | 
| `var x:tpe` | `x: tpe` |
| `def x: tpe` | `get x(): tpe` |
| `def x_=(v: tpe)` | `set x(v: tpe)` |
| `def x(...): tpe` | `x(...): tpe` |
