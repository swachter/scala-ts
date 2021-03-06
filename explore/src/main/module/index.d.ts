type ConvertParam<T> = 
  T extends scala.Option<infer I> ? I | undefined :
  T extends scala.collection.immutable.List<infer I> ? I[] :
  T

type ConvertParams<T> = T extends [...any[]] ? { [K in keyof T]: ConvertParam<T[K]>} : never
type ConvertFunction<F> = F extends (...args: infer P) => infer R ? (...ps: ConvertParams<P>) => ConvertParam<R> : never

type F1 = ConvertFunction<(l: scala.collection.immutable.List<number>) => scala.Option<boolean>>
// F1 = (l: number[]) => boolean | undefined

// export function convertFunction<F extends (...args: any[]) => any>(f: F): ConvertFunction<F>

export function greet(str: string): void;
export function random(): number;

export function multiParamLists1(a: number, b: number, c: number): number
export function multiParamLists2(a: number, b: number, c: number, d: number): number

export const maxInt: number;
export const maxLong: object;

export let globalVar: number

export class CaseClass {

    constructor(str: string);

    // a val translates into a readonly property
    readonly strVal: string;

}

export class StdClass {

    constructor(str: string);

    // a var translates into a property
    strVar: string;

    // a method defined without parentheses
    readonly upperProperty: string;

    get upperGetter(): string

    // a method defined with parentheses
    upperMethod(): string;

    set int(value: number);

    get int(): number;
}

interface StdClass2 {
  valueProperty: number
}

export class StdClass2 {

    constructor();

    set value(value: number);

    get value(): number;

    set numbers(value: number[]);

    get numbers(): number[];

    set option(value: number | undefined);

    get option(): number | undefined;

    set tuple(value: [string, number]);

    get tuple(): [string, number];

    [Symbol.iterator]<X>(x: number): Iterable<X>

}

export class ArrayAccess {

    constructor(v: number[], m: number[][])

    set vector(value: number[])

    get vector(): number[]

    set matrix(value: number[][])

    get matrix(): number[][]

}

export class JsClass {

    constructor(int: number);

    set int(value: number);

    get int(): number;

    static readonly staticVal: number
    static staticVar: number
    static staticDef(n: number): number
}

export const twice: (n: number) => number;

export const PromiseInterop: {

    sleepMillis(n: number): Promise<void>;

    onSuccess<T>(p: Promise<T>, f: (t: T) => void): void;
};

// declare an interface for a class and a separate interface for its constructor

export interface ConstrClass {
  '@f.q.n': never;
  readonly str: string;
}

export interface ConstrClass$Ctor {
        (str: string): ConstrClass
    new (str: string): ConstrClass
}

export const ConstrClass: ConstrClass$Ctor;

// behaves the same like ConstrClass / ConstrClass$Ctor

export class ConstrClass2 {
    '@f.q.n': never;
    constructor(str: string);
    readonly str: string;
}

export class ClassWithStatics {
    static readonly constant: number;
    static method(): void;
}

//

export interface Result$Base<L, R> {
  readonly isLeft: boolean;
  readonly isRight: boolean;
  readonly left: L;
  readonly right: R;
}

// define interfaces for Left and Right; declaration merging will merge these interfaces with the class declarations

export interface Left<L> extends Result$Base<L, never>{}
export interface Right<R> extends Result$Base<never, R>{}

export class Left<L> {
    // the tpe property can be used to match different cases with exhaustiveness check
    readonly tpe: 'Left';
    constructor(l: L);
}

export class Right<R> {
    readonly tpe: 'Right';
    constructor(r: R);
}

export type Result<L, R> = Left<L> | Right<R>

export type ADT = Case1 | Case2

export class Case1 {

    constructor(str: string);

    readonly literalTypedInt: 1;
    readonly literalTypedString: 'a';
    readonly literalTypedBoolean: false;
    readonly caseId: number;

}

export class Case2 {

    constructor(str: string);

    readonly literalTypedInt: 2;
    readonly literalTypedString: 'b';
    readonly literalTypedBoolean: true;
    readonly caseId: number;

}

// all non-ScalaJS-exported types are opaque to TypeScript
// -> such types can be referenced in the exported API
// -> export nominal interfaces without any methods for these types
export namespace scala {

  interface Option<T> {
    'scala.Option': never
  }
  interface Some<T> extends Option<T> {
    'scala.Some': never
  }
  interface None extends Option<never> {
    'scala.None': never
  }

  namespace collection {

    interface Iterable<V> {
      'scala.collection.Iterable': never
    }

    interface Map<K,V> {
      'scala.collection.Map': never
    }

    namespace mutable {

      interface Map<K,V> extends scala.collection.Map<K, V> {
        'scala.collection.mutable.Map': never
      }

    }

    namespace immutable {

      interface Iterable<V> extends scala.collection.Iterable<V> {
        'scala.collection.immutable.Iterable': never
      }

      interface Seq<V> extends scala.collection.immutable.Iterable<V> {
        'scala.collection.immutable.Seq': never
      }

      interface List<V> extends scala.collection.immutable.Seq<V> {
        'scala.collection.immutable.List': never
      }

      interface Map<K,V> extends scala.collection.Map<K, V> {
        'scala.collection.immutable.Map': never
      }
    }
  }
}

export const stdLibInterOp: {

  toOption<T>(t: T | undefined): scala.Option<T>
  fromOption<T>(o: scala.Option<T>): T | undefined
  toSome<T>(t: T): scala.Some<T>
  readonly none: scala.None

  // the map wraps the dictionary
  asMap<V>(dict: { [key: string]: V}): scala.collection.mutable.Map<string, V>
  addToMap<K, V>(key: K, value: V, map: scala.collection.mutable.Map<K, V>): void
  // the map is converted into a new dictionary
  toDictionary<V>(map: scala.collection.Map<string, V>): { [key: string]: V }

  list<V>(...v: V[]): scala.collection.immutable.List<V>
  noneEmptyList<V>(one: V, ...v: V[]): scala.collection.immutable.List<V>

  // construct an immutable map from a varargs tuples
  immutableMap<K, V>(...kv: [K, V][]): scala.collection.immutable.Map<K, V>
}

export class A {
  constructor(n: number)
  readonly n: number
}

export class B extends A {
  constructor(n: number, s: string)
  readonly s: string
}

export interface Base {
  doIt(): void
  someNumber(): number
}

export interface Derived extends Base {}

export class Derived {
  constructor()
}

export interface Formatter<X> {
  format(x: X): string
}

export interface BooleanFormatter extends Formatter<boolean> {}
export interface IntFormatter extends Formatter<number> {}

export class BooleanFormatter {
  constructor()
  readonly tpe: 'b'
}

export class IntFormatter {
  constructor()
  readonly tpe: 'i'
}

export type FormatterUnion = BooleanFormatter | IntFormatter

export interface NonExportedJsObject {
  readonly name: string
}

export interface NonExportedJsClass {
  readonly name: string
}

export const nonExportedJsObject: NonExportedJsObject
export function nonExportedJsClass(): NonExportedJsClass

export const outerObject: {
  a: string,
  o2: {
    b: string
  }
}

export class ToRange {
  constructor(from: number, to: number)
  [Symbol.iterator]: () => Iterator<number>
}

export class ClassWithMethodsFromTraits {
  constructor()
  base(n: number): number
  middle(n: number): number
}

export class ClassWithInnerObj {
  constructor()
  readonly x: number
  readonly innerObj: {
    y: number
  }
}

export interface AdapterV1$ {
  readonly x: _root_AdapterV1.x$
  'AdapterV1$': never
}
export const AdapterV1: AdapterV1$
export namespace AdapterV1 {
  interface InstanceAdapter<D> {
    readonly $delegate: D
    'AdapterV1.InstanceAdapter': never
  }
  interface x$ {
    readonly OuterV1: _root_AdapterV1.x.OuterV1$
    readonly outerV1: AdapterV1.x.OuterV1
    'AdapterV1.x$': never
  }
  namespace x {
    interface OuterV1 extends AdapterV1.InstanceAdapter<_root_x.OuterV1> {
      readonly x: number
      readonly MiddleV1: _root_AdapterV1.x.OuterV1.MiddleV1$
      readonly middleV1: _root_AdapterV1.x.OuterV1.MiddleV1$
      'AdapterV1.x.OuterV1': never
    }
    interface OuterV1$ {
      newInstance(x: number): _root_x.OuterV1
      newAdapter(d: _root_x.OuterV1): AdapterV1.x.OuterV1
      'AdapterV1.x.OuterV1$': never
    }
    namespace OuterV1 {
      interface MiddleV1 extends AdapterV1.InstanceAdapter<_root_x.OuterV1.MiddleV1> {
        readonly y: string
        readonly InnerV1: _root_AdapterV1.x.OuterV1.MiddleV1.InnerV1$
        readonly innerV1: _root_AdapterV1.x.OuterV1.MiddleV1.InnerV1$
        'AdapterV1.x.OuterV1.MiddleV1': never
      }
      interface MiddleV1$ {
        newInstance(y: string): _root_x.OuterV1.MiddleV1
        newAdapter(d: _root_x.OuterV1.MiddleV1): _root_AdapterV1.x.OuterV1.MiddleV1
        'AdapterV1.x.OuterV1.MiddleV1$': never
      }
      namespace MiddleV1 {
        interface InnerV1 extends AdapterV1.InstanceAdapter<_root_x.OuterV1.MiddleV1.InnerV1> {
          readonly z: boolean
          'AdapterV1.x.OuterV1.MiddleV1.InnerV1': never
        }
        interface InnerV1$ {
          newInstance(z: boolean): _root_x.OuterV1.MiddleV1.InnerV1
          newAdapter(d: _root_x.OuterV1.MiddleV1.InnerV1): _root_AdapterV1.x.OuterV1.MiddleV1.InnerV1
          'AdapterV1.x.OuterV1.MiddleV1.InnerV1$': never
        }
      }
    }
  }
}
export namespace x {
  interface OuterV1 {
    'x.OuterV1': never
  }
  namespace OuterV1 {
    interface MiddleV1 {
      'x.OuterV1.MiddleV1': never
    }
    namespace MiddleV1 {
      interface InnerV1 {
        'x.OuterV1.MiddleV1.InnerV1': never
      }
    }
  }
}
import _root_AdapterV1 = AdapterV1
import _root_x = x