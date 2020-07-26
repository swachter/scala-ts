import * as fs from 'from-scala';

fs.greet('Bob');

console.log('random: ' + fs.random());

console.log(`multiParamLists1: ${fs.multiParamLists1(1, 2, 3)}`)
console.log(`multiParamLists2: ${fs.multiParamLists2(1, 2, 3, 4)}`)

console.log('maxInt: ' + fs.maxInt);
console.log('typeof maxInt: ' + typeof(fs.maxInt))

console.log('maxLong: ' + fs.maxLong);
console.log('typeof maxLong: ' + typeof(fs.maxLong))

console.log(`globalVar: ${fs.globalVar}`)

const cc = new fs.CaseClass('abc');
console.log('cc: ' + cc);
console.log('cc.strVal: ' + cc.strVal);

const sc = new fs.StdClass('abc');
console.log('sc: ' + sc);
console.log('sc.strVar: ' + sc.strVar);
sc.strVar = 'uvw';
console.log('sc.strVar: ' + sc.strVar);
console.log(`sc instanceof fs.StdClass: ${sc instanceof fs.StdClass}`)
console.log(`fs.StdClass: ${fs.StdClass}`)

console.log('sc.int: ' + sc.int);
sc.int = 55;
console.log('sc.int: ' + sc.int);

console.log('sc.upperProperty: ' + sc.upperProperty);
console.log('sc.upperGetter  : ' + sc.upperGetter);
console.log('sc.upperMethod  : ' + sc.upperMethod());


const jc = new fs.JsClass(5);
console.log('jc.int: ' + jc.int);
jc.int = 55;
console.log('jc.int: ' + jc.int);
console.log(`staticVal: ${fs.JsClass.staticVal}`)
console.log(`staticVar: ${fs.JsClass.staticVar}`)
fs.JsClass.staticVar += 1
console.log(`staticVar: ${fs.JsClass.staticVar}`)
console.log(`staticDef: ${fs.JsClass.staticDef(2)}`)

console.log('twice(2): ' + fs.twice(2));

const p1 = fs.PromiseInterop.sleepMillis(500);
fs.PromiseInterop.onSuccess(p1.then(() => 'abc'), (str: string) => console.log(`promise resolved to: ${str}`));

console.log(`ConstrClass: ${fs.ConstrClass}`);
console.log(`typeof ConstrClass: ${typeof fs.ConstrClass}`);
const constrClass = new fs.ConstrClass('abc');
console.log(`constrClass instanceof ConstrClass: ${constrClass instanceof fs.ConstrClass}`);
console.log(`constrClass.str:  ${constrClass.str}`);

console.log(`ConstrClass2: ${fs.ConstrClass2}`);
const constrClass2 = new fs.ConstrClass2('abc');
console.log(`constrClass2 instanceof ConstrClass : ${constrClass2 instanceof fs.ConstrClass}`);
console.log(`constrClass2 instanceof ConstrClass2: ${constrClass2 instanceof fs.ConstrClass2}`);
console.log(`constrClass2.str:  ${constrClass2.str}`);

class TypeScriptClass {
  constructor(readonly str: string) {}
}

const typeScriptClass = new TypeScriptClass('uvw');
console.log(`typeScriptClass.str: ${typeScriptClass.str}`);
console.log(`typeScriptClass instanceof TypeScriptClass: ${typeScriptClass instanceof TypeScriptClass}`);
console.log(`typeScriptClass: ${TypeScriptClass}`);

console.log(`ClassWithStatics.constant: ${fs.ClassWithStatics.constant}`);
fs.ClassWithStatics.method();

function assertNever(n: never): never {
    throw new Error('never case reached')
}

function test<L, R>(res: fs.Result<L, R>) {
    if (res.isLeft) {
      console.log(`left: ${res.left}`);
    } else if (res.isRight) {
      console.log(`right: ${res.right}`);
    } else {
      console.log(`unexpected`);
    }
    if (res instanceof fs.Left) {
      console.log('left');
    } else if (res instanceof fs.Right) {
      console.log('right');
    } else {
      assertNever(res);
    }
    if (res.tpe === 'Left') {
      console.log('left2');
    } else if (res.tpe === 'Right') {
      console.log('right2');
    } else {
      assertNever(res);
    }
    console.log(`res instanceof Left: ${res instanceof fs.Left}`);
    console.log(`res instanceof Right: ${res instanceof fs.Right}`);
}

const left = new fs.Left('abc');
const right = new fs.Right(42);

test(left);
test(right);

const strOption: fs.scala.Option<string> = fs.stdLibInterOp.toOption('abc')
console.log(`strOption: ${strOption}`)
console.log(`none: ${fs.stdLibInterOp.toOption(undefined)}`)
const booleanOption: fs.scala.Option<boolean> = fs.stdLibInterOp.toOption(true)
console.log(`booleanOption: ${booleanOption}`)
const someNumber: fs.scala.Some<number> = fs.stdLibInterOp.toSome(333)
const theNumber: number | undefined = fs.stdLibInterOp.fromOption(someNumber)
console.log(`theNumber: ${theNumber}`)

const dict1: { [key: string]: number } = { 'a': 1, 'b': 2 }
const map = fs.stdLibInterOp.asMap(dict1)
fs.stdLibInterOp.addToMap('c', 3, map)
console.log(`map: ${map}`)
console.log(`dict1: ${JSON.stringify(dict1)}`)
const dict2 = fs.stdLibInterOp.toDictionary(map)
console.log(`dict2: ${JSON.stringify(dict2)}`)
dict2['u'] = 77
console.log(`dict2: ${JSON.stringify(dict2)}`)
// 'u' -> 77 is not contained in dict1
console.log(`dict1: ${JSON.stringify(dict1)}`)

const list = fs.stdLibInterOp.list(1, 2, 3)
console.log(`list: ${list}`)

const nonEmptyList = fs.stdLibInterOp.list(0, 1, 2, 3)
console.log(`nonEmptyList: ${nonEmptyList}`)

const immMap = fs.stdLibInterOp.immutableMap(['a', 97], ['b', 98], ['c', 99])
console.log(`immMap: ${immMap}`)
const immMapDict = fs.stdLibInterOp.toDictionary(immMap)
console.log(`immMapDict: ${JSON.stringify(immMapDict)}`)

const a = new fs.A(7)
console.log(`a.n: ${a.n}`)
const b = new fs.B(8, 'abc')
console.log(`b.n: ${b.n}`)
console.log(`b.s: ${b.s}`)

const derived = new fs.Derived()
derived.doIt()
console.log(`derived.someNumber(): ${derived.someNumber()}`)

const booleanFormatter = new fs.BooleanFormatter()
const intFormatter = new fs.IntFormatter()

console.log(`booleanFormatter: ${booleanFormatter.format(true)}`)
console.log(`intFormatter: ${intFormatter.format(777)}`)

function genFormat(formatter: fs.FormatterUnion) {
    if (formatter.tpe === 'b') {
        console.log(`bf: ${formatter.format(true)}`)
    } else if (formatter.tpe === 'i') {
        console.log(`bf: ${formatter.format(666)}`)
    } else {
        assertNever(formatter)
    }
}

genFormat(booleanFormatter)
genFormat(intFormatter)

console.log(`fs.outerObject.a: ${fs.outerObject.a}`)
console.log(`fs.outerObject.o2.b: ${fs.outerObject.o2.b}`)