import * as m from 'scala-adapter'
import { newAdapter } from './util'

describe('package object', function () {

  it('access method from package object', function () {
    expect(m.Adapter.eu.swdev.scala.ts.adapter.test.min([7, 6, 8])).toBe(6)
  })

})