import * as m from 'scala-ts-mod'
import {assertNever} from './util'

describe('object adt', function() {

  function match(adt: m.e2e.ObjectAdt.Adt$u): string | number {
    if (adt.tpe === 1) {
      return adt.str
    } else if (adt.tpe === 2) {
      return adt.num
    } else {
      return assertNever(adt)
    }
  }

  it('read val', function() {
    expect(match(m.ObjectAdtCase1)).toBe('abc')
    expect(match(m.ObjectAdtCase2)).toBe(555)
  });

});