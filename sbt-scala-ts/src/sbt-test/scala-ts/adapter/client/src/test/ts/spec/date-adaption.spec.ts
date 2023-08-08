import * as m from '../mod/scala-ts-mod'

describe('date access', function () {

  // class adapter
  const da = m.Adapter.t.DateAdaption

  it('as JavaScript date', function () {
    const d = new Date()
    da.date = d
    expect(da.date).toStrictEqual(d)
  })

  it('as number', function () {
    const d = new Date().getTime()
    da.doubleDate = d
    expect(da.doubleDate).toBe(d)
  })

})