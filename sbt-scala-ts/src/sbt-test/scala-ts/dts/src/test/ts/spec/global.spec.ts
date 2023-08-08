import * as m from '../mod/scala-ts-mod'

describe('WeakMap', function() {

  it('construct', function() {
    const map = new WeakMap<object, string>()
    const k1 = {}
    const k2 = {}
    m.setInWeakMap(k1, 'abc', map)
    m.setInWeakMap(k2, 'uvw', map)
    expect(map.get(k1)).toBe('abc')
    expect(map.get(k2)).toBe('uvw')
  });

});