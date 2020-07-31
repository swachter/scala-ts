import * as m from 'from-scala'

describe('class with inner obj', () => {

  it('inner object can be accessed', () => {
    const c = new m.ClassWithInnerObj()
    expect(c.x).toBe(1)
    // TODO uncomment after issue is fixed
    // cf. https://github.com/scala-js/scala-js/issues/4142
    //expect(c.innerObj.y).toBe(2)
    expect(c.innerObj).toBeUndefined
  })

})