import * as m from '../mod/scala-ts-mod'

describe('nested objects', function() {

  it('object/object/object', function() {
    expect(m.OuterObject.middle.innerMost.x).toBe(1)
    m.OuterObject.middle.innerMost.x += 1
    expect(m.OuterObject.middle.innerMost.x).toBe(2)
  })

  it('class/object', function() {
    const c = new m.OuterClass()
    expect(c.mid.x).toBe(1)
    c.mid.x  += 1
    expect(c.mid.x).toBe(2)
  })

})