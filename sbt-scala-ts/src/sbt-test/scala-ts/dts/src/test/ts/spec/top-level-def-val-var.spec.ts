import * as m from '../mod/scala-ts-mod'

describe('top level', function() {

  it('read val', function() {
    expect(m.immutable).toBe(5)
  });

  it('read var', function() {
    expect(m.mutable).toBe('abc')
  });

  it('write var', function() {
    const oldValue = m.mutable
    const newValue = oldValue + oldValue
    m.setMutable(newValue)
    expect(m.mutable).toBe(newValue)
  });

  it('call function', function() {
    expect(m.multiply(2, 3)).toBe(6)
  });

});