import * as m from 'scala-ts-mod'

describe('js.Date', function() {

  it('construct', function() {
    const d = new Date('2020-06-08')
    expect(m.fullYearOfDate(d)).toBe(2020)
  });

});