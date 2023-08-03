import * as sm from 'scala-ts-mod'
import j from 'js-joda'

describe('js-joda', function() {

  it('duration', function() {
    const nbDays1 = 1
    const ld1 = j.LocalDate.now()
    const ld2 = ld1.plusDays(nbDays1)
    const diff1 = ld1.until(ld2)
    expect(diff1.days()).toBe(nbDays1)
    const nbDays2 = 3
    const ld3 = sm.addDays(ld1, nbDays2)
    const diff2 = ld1.until(ld3)
    expect(diff2.days()).toBe(nbDays2)
  });

});