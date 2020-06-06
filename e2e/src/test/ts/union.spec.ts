import * as m from 'scala-ts-mod'

describe('union', function() {

    it('simple', function() {
      expect(m.invert(1)).toBe(-1)
      expect(m.invert('abc')).toBe('cba')
      expect(m.invert(true)).toBe(false)
    });

});
