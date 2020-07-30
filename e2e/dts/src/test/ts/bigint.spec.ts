import * as m from 'scala-ts-mod'

describe('abstract', function() {

  it('simple', function() {
    const digits = 30
    const s1 = '9'.repeat(digits)
    const b1 = m.BigIntInterop.string2BigInt(s1)
    const b2 = b1 + 1n
    const s2 = m.BigIntInterop.bigInt2String(b2)
    const e2 = '1' + '0'.repeat(digits)
    expect(s2).toBe(e2)
  })

})