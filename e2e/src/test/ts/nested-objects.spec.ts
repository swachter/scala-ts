import * as m from 'scala-ts-mod'

describe('nested objects', function() {

  it('simple', function() {
    expect(m.OuterObject.middle.innerMost.x).toBe(1)
    m.OuterObject.middle.innerMost.x += 1
    expect(m.OuterObject.middle.innerMost.x).toBe(2)
  })

})