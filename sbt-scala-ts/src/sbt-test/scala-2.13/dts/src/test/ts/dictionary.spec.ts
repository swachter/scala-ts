import * as m from 'scala-ts-mod'

describe('js.dictionary', function() {

  it('construct', function() {
    const d = { 'a': 1, 'b': 2, 'c': 3}
    expect(m.sumDict(d)).toBe(6)
    m.addToDict('d', 4, d)
    expect(m.sumDict(d)).toBe(10)
  });

});