export function greet(str: string): void;
export function random(): number;

export const maxInt: number;
export const maxLong: object;

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

    // a method defined with parentheses
    upperMethod(): string;

    set int(value: number);

    get int(): number;
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

}

export class JsClass {

    constructor(int: number);

    set int(value: number);

    get int(): number;
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

// all non-ScalaJS-exported types are opaque to TypeScript
// -> such types can be referenced in the exported API
// -> export nominal interfaces without any methods for these types
export namespace scala {
  interface Option<T> {
    '@scala.Option': never
  }
  interface Some<T> extends Option<T> {
    '@scala.Some': never
  }
  interface None extends Option<never> {
    '@scala.None': never
  }
}

export const stdLibInterOp: {
  toOption<T>(t: T | undefined): scala.Option<T>
  fromOption<T>(o: scala.Option<T>): T | undefined
  toSome<T>(t: T): scala.Some<T>
  readonly none: scala.None
}
