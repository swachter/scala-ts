import * as m from 'scala-ts-mod'

describe('static', function() {

  it('simple', function() {
    expect(m.ClassWitStatics.twice(2)).toBe(4)
    expect(m.ClassWitStatics.str).toBe('abc')
    expect(m.ClassWitStatics.numero).toBe(55)
    m.ClassWitStatics.numero = 66
    expect(m.ClassWitStatics.numero).toBe(66)
    expect(m.ClassWitStatics.x).toBe(0)
    m.ClassWitStatics.x += 1
    expect(m.ClassWitStatics.x).toBe(1)
  })

})