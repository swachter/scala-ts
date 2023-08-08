import * as m from '../mod/scala-ts-mod'

describe('js class', function() {

  it('construct', function() {
    const initialStr = 'abc'
    const initialNum = 5
    const c = new m.JsClass(initialStr, initialNum)
    expect(c.initialStr).toBe(initialStr)
    expect(c.num).toBe(initialNum)
    expect(c.str).toBe(initialStr)
  });

  it('modify', function() {
    const initialStr = 'abc'
    const initialNum = 5
    const c = new m.JsClass(initialStr, initialNum)
    const s = initialStr + initialStr
    c.str = s
    expect(c.str).toBe(s)
    c.doubleNum()
    expect(c.num).toBe(2 * initialNum)
    c.num = initialNum
    expect(c.num).toBe(initialNum)
  });

});