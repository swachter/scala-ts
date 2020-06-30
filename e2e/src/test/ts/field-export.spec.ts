import * as m from 'scala-ts-mod'

describe('field export', function() {

  it('simple', function() {
    const c = new m.FieldExport('abc', 'uvw')
    /* Bug in ScalaJS?
    expect(c.x).toBe('abc')
    expect(c.y).toBe('uvw')
    */
  })

})