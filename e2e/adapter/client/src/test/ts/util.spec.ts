import * as m from 'scala-adapter'

describe('util', function() {

  it('sequence list of options', function() {
    expect(m.Adapter.x.Util.sequence([1, 2])).toStrictEqual([1, 2])
    expect(m.Adapter.x.Util.sequence([1, undefined])).toBeUndefined
    expect(m.Adapter.x.Util.sequence([undefined, 2])).toBeUndefined
    expect(m.Adapter.x.Util.sequence([undefined, undefined])).toBeUndefined
  })

})