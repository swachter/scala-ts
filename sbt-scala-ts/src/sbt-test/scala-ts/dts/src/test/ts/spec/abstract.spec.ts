import * as m from '../mod/scala-ts-mod'

describe('abstract', function() {

  it('simple', function() {
    const c1 = new m.AbstractTestCase1('abc')
    expect(c1.x).toBe('abc')
    const c2 = new m.AbstractTestCase2(55)
    expect(c2.x).toBe(55)

  })

})