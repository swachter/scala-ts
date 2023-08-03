import * as m from 'scala-ts-mod'

describe('function', function() {

    it('function0', function() {
      expect(m.fun0(() => 'a')).toBe('a')
      expect(m.fun0(() => 1)).toBe(1)
    });
  
    it('function1', function() {
      // fun1 is a flatMap
      expect(m.fun1([1, 2, 3], n => new Array(n).fill(n))).toStrictEqual([1, 2, 2, 3, 3, 3])
    });
  
    it('function2', function() {
      // fun2 is a zipMap
      expect(m.fun2([1, 2, 3], ['a', 'b', 'c'], (n, s) => s.repeat(n))).toStrictEqual(['a', 'bb', 'ccc'])
    });
  
  });