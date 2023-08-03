import * as m from 'scala-ts-mod'

describe('iterable', function() {

  it('simple', function() {
    const range = new m.FromToRange(0, 4)
    let s = 0
    for (let n of range) {
      s += n
    }
    expect(s).toBe(10)
  })

})