import * as m from 'scala-adapter'
import { newAdapter } from './util'

describe('simple class', function () {

  // class adapter
  const sca = m.Adapter.x.y.SimpleClass

  it('access constructor var', function () {
    const d = sca.newDelegate([1, 2, 3])
    const a = sca.newAdapter(d)
    expect(a.x).toStrictEqual([1, 2, 3])
    expect(a.sum).toBe(6)
    a.x = [4, 5, 6]
    expect(a.x).toStrictEqual([4, 5, 6])
    expect(a.sum).toBe(15)
    a.filter(n => n % 2 === 0)
    expect(a.sum).toBe(10)
  })

  it('combine instance creation and adaption', () => {
    const c = newAdapter(sca, [1, 2, 3, 4, 5])
    expect(c.sum).toBe(15)
  })

})