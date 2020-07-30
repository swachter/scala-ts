import * as m from 'scala-ts-mod'

describe('js.dictionary', function() {

  it('construct', function() {
    const r = m.createRegExp('a+b+')
    expect(m.regExpMatches(r, 'ab')).toBeTruthy()
    expect(m.regExpMatches(r, 'ac')).toBeFalsy()
  });

});