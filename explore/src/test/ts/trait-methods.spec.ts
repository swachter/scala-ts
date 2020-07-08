import * as m from 'from-scala'

describe('trait methods', function() {

  it('simple', function() {
    const c = new m.ClassWithMethodsFromTraits()
    expect(c.base(2)).toBe(4)
    expect(c.middle(2)).toBe(6)
  });

});