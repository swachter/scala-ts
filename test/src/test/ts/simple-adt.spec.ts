import * as m from 'scala-ts-mod'
import {assertNever} from './util'

describe('simple adt', function() {

  function match(adt: m.eu.swdev.scala.ts.sample.p1.SimpleAdt.Adt$): string | number {
    if (adt.tpe === 's') {
      return adt.str
    } else if (adt.tpe === 'i') {
      return adt.int
    } else {
      return assertNever(adt)
    }
  }

  it('read val', function() {
    expect(match(new m.SimpleAdtCase1(5))).toBe(5)
    expect(match(new m.SimpleAdtCase2('x'))).toBe('x')
  });

});