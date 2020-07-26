import * as m from 'from-scala'

describe('property', function() {

  it('access value via accessors and via property', function() {
    const i = new m.StdClass2()
    expect(i.value).toBe(5)
    i.value = 6
    expect(i.value).toBe(6)
    expect(i.valueProperty).toBe(6)
    i.valueProperty = 7
    expect(i.value).toBe(7)
    expect(i.valueProperty).toBe(7)
  });

  it('array', () => {
    const i = new m.StdClass2()
    expect(i.numbers.length).toBe(5)
    i.numbers = [66, 67, 68]
    expect(i.numbers.length).toBe(3)
    expect(i.numbers[0]).toBe(66)
    expect(i.numbers[1]).toBe(67)
    expect(i.numbers[2]).toBe(68)
  })

  it('option', () => {
    const i = new m.StdClass2()
    expect(i.option).toBe(5)
    i.option = undefined
    expect(i.option).toBeUndefined()
    i.option = 68
    expect(i.option).toBe(68)
  })

  it('tuple', () => {
    const i = new m.StdClass2()
    expect(i.tuple).toStrictEqual(['abc', 1])
    i.tuple = ['uvw', 2]
    expect(i.tuple).toStrictEqual(['uvw', 2])
  })

})