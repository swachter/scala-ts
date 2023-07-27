import * as m from 'scala-ts-mod'

describe('varargs', function() {


    it('sum', function() {
      expect(m.sumVarArgs(1, 2, 3, 4)).toBe(10)
    });

    it('create dictionary', function() {
      const d = m.createDictionary(['a', 1], ['b', 2], ['c', 3])
      expect(d.a).toBe(1)
      expect(d.b).toBe(2)
      expect(d.c).toBe(3)
    });

});
