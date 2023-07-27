import * as m from 'scala-ts-mod'

describe('Promise and PromiseLike', function() {

  function promise<T>(t?: T): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        process.nextTick(() => t ? resolve(t) : reject(t))
    })
  }

  it('map Promise', () => {
    expect.assertions(1)
    const p = promise(1)
    const q = m.mapPromise(p, x => 2*x)
    return expect(q).resolves.toBe(2)
  });

  it('map Promise (neg)', () => {
    expect.assertions(1)
    const p = promise(1)
    const q = m.mapPromise(p, x => 2*x)
    return expect(q).resolves.not.toBe(3)
  });

  it('map PromiseLike', () => {
    expect.assertions(1)
    const p = promise(1)
    const q = m.mapPromiseLike(p, x => 2*x)
    return expect(q).resolves.toBe(2)
  });

});