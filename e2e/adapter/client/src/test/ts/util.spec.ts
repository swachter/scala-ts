import * as m from 'scala-adapter'

describe('util', function() {

  it('traverse list of options', function() {
    expect(m.Adapter.x.Util.traverse([1, 2])).toStrictEqual([1, 2])
    expect(m.Adapter.x.Util.traverse([1, undefined])).toBeUndefined
    expect(m.Adapter.x.Util.traverse([undefined, 2])).toBeUndefined
    expect(m.Adapter.x.Util.traverse([undefined, undefined])).toBeUndefined
  })

})