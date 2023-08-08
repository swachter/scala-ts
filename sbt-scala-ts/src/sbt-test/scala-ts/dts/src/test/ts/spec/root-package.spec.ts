import * as m from '../mod/scala-ts-mod'

describe('root-package', function() {

  it('class', function() {
    const c = new m.RootPackageClass()
    expect(c.i).toBe(1)
  });

  it('object', function() {
    expect(m.RootPackageObject.x).toBe(1)
  });

});