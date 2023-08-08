import * as m from '../mod/scala-ts-mod'

describe('iterator', function() {


    it('sum', function() {
      expect(m.sumIterable([1, 2, 3, 4])).toBe(10)
    });

    it('create dictionary', function() {
      const i = m.numberIterator(1)
      let n = i.next()
      expect(n.done).toBe(false)
      expect(n.value).toBe(0)
      n = i.next()
      expect(n.done).toBe(false)
      expect(n.value).toBe(1)
      n = i.next()
      expect(n.done).toBe(true)
    });

});
