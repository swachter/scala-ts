import * as m from '../mod/scala-ts-mod'

describe('union', function() {

  function invert(u: m.e2e.UnionTest.U) {
     return m.invert(u)
  }

    it('simple', function() {
      expect(m.invert(1)).toBe(-1)
      expect(m.invert('abc')).toBe('cba')
      expect(m.invert(true)).toBe(false)
    });

    it('use union type alias', function() {
      expect(invert(5)).toBe(-5)
    });

});
