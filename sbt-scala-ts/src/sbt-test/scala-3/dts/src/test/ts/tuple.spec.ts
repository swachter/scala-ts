import * as m from 'scala-ts-mod'

describe('tuple', function() {

    it('tuple a function', function() {
      const f = (n1: number, n2: number) => n1 + n2
      const tf = m.tupleFunction(f)
      expect(tf([1, 2])).toBe(3)
    });
  
  });