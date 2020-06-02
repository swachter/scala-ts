import * as m from 'from-scala'
import { assertNever } from './util'

describe('adt', function() {

  function checkByLiteralInt(adt: m.ADT, caseId: number): void {
    if (adt.literalTypedInt === 1) {
        expect(adt.caseId).toBe(1)
    } else if (adt.literalTypedInt === 2) {
        expect(adt.caseId).toBe(2)
    } else {
        assertNever(adt)
    }
  }

  function checkByLiteralString(adt: m.ADT, caseId: number): void {
    if (adt.literalTypedString === 'a') {
        expect(adt.caseId).toBe(1)
    } else if (adt.literalTypedString === 'b') {
        expect(adt.caseId).toBe(2)
    } else {
        assertNever(adt)
    }
  }

  function checkByLiteralBoolean(adt: m.ADT, caseId: number): void {
    if (adt.literalTypedBoolean === false) {
        expect(adt.caseId).toBe(1)
    } else if (adt.literalTypedBoolean === true) {
        expect(adt.caseId).toBe(2)
    } else {
        assertNever(adt)
    }
  }

  it('discriminate based on number', function() {
    checkByLiteralInt(new m.Case1(''), 1)
    checkByLiteralInt(new m.Case2(''), 2)
  });

  it('discriminate based on string', function() {
    checkByLiteralString(new m.Case1(''), 1)
    checkByLiteralString(new m.Case2(''), 2)
  });

  it('discriminate based on boolean', function() {
    checkByLiteralBoolean(new m.Case1(''), 1)
    checkByLiteralBoolean(new m.Case2(''), 2)
  });


});