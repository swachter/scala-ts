import * as m from 'scala-adapter'
import { newAdapter } from './util'

describe('nested class', function () {

  // class adapter
  const ca = m.Adapter.o.OuterClass

  it('two step construction', function () {
    const dOuter = ca.newDelegate(5)
    const aOuter = ca.newAdapter(dOuter)
    expect(aOuter.x).toBe(5)
    const dInner = aOuter.Inner.newDelegate('abc')
    const aInner = aOuter.Inner.newAdapter(dInner)
    expect(aInner.y).toBe('abc')
  })

  it('combined construction', () => {
    const outer = newAdapter(ca, 1)
    expect(outer.x).toBe(1)
    const inner = newAdapter(outer.Inner, 'uvw')
    expect(inner.y).toBe('uvw')
  })

})