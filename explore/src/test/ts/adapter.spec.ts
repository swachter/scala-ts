import * as m from 'from-scala'
import { assertNever } from './util'

describe('adapter', function() {

  it('instantiate inner classes', function() {
    const outer = m.AdapterV1.x.OuterV1.newInstance(5)
    const outerAdapter = m.AdapterV1.x.OuterV1.newAdapter(outer)
    expect(outerAdapter.x).toBe(5)

    const middle = outerAdapter.middleV1.newInstance('abc')
    const middleAdapter = outerAdapter.middleV1.newAdapter(middle)
    expect(middleAdapter.y).toBe('abc')

    const inner = middleAdapter.innerV1.newInstance(true)
    const innerAdapter = middleAdapter.innerV1.newAdapter(inner)
    expect(innerAdapter.z).toBe(true)

  });


});