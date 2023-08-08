import * as m from '../mod/scala-ts-mod'

describe('bracket notation', function() {

  it('simple', function() {
    expect(m.BracketNotation['!a']).toBe(1)
    expect(m.BracketNotation['!b']).toBe(1)
    m.BracketNotation['!b'] += 1
    expect(m.BracketNotation['!b']).toBe(2)
    expect(m.BracketNotation['!c']()).toBe(1)
    expect(m.BracketNotation['!d'](3)).toBe(3)
    expect(m.BracketNotation['!e']['!f']).toBe(1)
  })

})