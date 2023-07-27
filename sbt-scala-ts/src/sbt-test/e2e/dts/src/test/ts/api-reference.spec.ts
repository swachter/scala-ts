import * as m from 'scala-ts-mod'

describe('api reference', function() {

  it('class-2-class', function() {
    expect.assertions(1)
    const o = m.convertInput2Output(m.createInput(0))
    if ('i' in o) {
      expect(o.i).toBe(0)
    }
  });

  it('class-2-class', function() {
    expect.assertions(1)
    const o = m.convertInput2Output(m.createInput(1))
    if ('s' in o) {
      expect(o.s).toBe('abc')
    }
  });


});