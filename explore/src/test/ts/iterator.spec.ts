import * as m from 'from-scala'
import { assertNever } from './util'

describe('iterator', function() {

  it('discriminate based on number', function() {
    const range = new m.ToRange(0, 5)
    let sum = 0
    for (let n of range) {
      sum += n
    }
    expect(sum).toBe(15)
  })

})

