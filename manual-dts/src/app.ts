import * as fs from 'from-scala';

fs.greet('Bob');

console.log('random: ' + fs.random());

console.log('maxInt: ' + fs.maxInt);
console.log('typeof maxInt: ' + typeof(fs.maxInt))

console.log('maxLong: ' + fs.maxLong);
console.log('typeof maxLong: ' + typeof(fs.maxLong))

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
console.log('sc.upperMethod: ' + sc.upperMethod());

const sc2 = new fs.StdClass2();
console.log('sc2.value: ' + sc2.value);
sc2.value = 66;
console.log('sc2.value: ' + sc2.value);
console.log(`typeof sc2: ${typeof sc2}`);

console.log('sc2.numbers: ' + sc2.numbers);
sc2.numbers = [66, 67, 68];
console.log('sc2.numbers: ' + sc2.numbers);

console.log('sc2.option: ' + sc2.option);
sc2.option = undefined;
console.log('sc2.option: ' + sc2.option);
sc2.option = 77;
console.log('sc2.option: ' + sc2.option);

console.log('sc2.tuple: ' + sc2.tuple);
const [s, n] = sc2.tuple;
console.log(`s: ${s}`);
console.log(`n: ${n}`);
sc2.tuple = ['uvw', 68];
console.log('sc2.tuple: ' + sc2.tuple);

const jc = new fs.JsClass(5);
console.log('jc.int: ' + jc.int);
jc.int = 55;
console.log('jc.int: ' + jc.int);

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
    return n;
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
