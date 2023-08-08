import * as m from '../mod/scala-ts-mod'

describe('ctor param export', function() {

  it('simple', function() {
    const c = new m.CtorParamExport('abc', 'uvw')
    expect(c.x).toBe('abc')
    expect(c.y).toBe('uvw')
  })

})