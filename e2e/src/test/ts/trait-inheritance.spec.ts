import * as m from 'scala-ts-mod'

describe('trait inheritance', function() {

  it('non js.Object', function() {
    const c = new m.ClassWithInheritedMethods1()
    expect(c.base(1)).toBe(2)
    expect(c.middle(2)).toBe(6)
  })

  it('js.Object', function() {
    const c = new m.ClassWithInheritedMethods2()
    expect(c.base(1)).toBe(2)
    expect(c.middle(2)).toBe(6)
  })

})