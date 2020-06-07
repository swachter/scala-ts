import * as m from 'from-scala'

describe('non-exported js.Object', function() {

  it('access js.Object member', function() {
    expect(m.nonExportedJsObject.name).toBe('nonExportedJsObject')
  });

  it('access class derived from js.Object member', function() {
    expect(m.nonExportedJsClass().name).toBe('nonExportedJsClass')
  });

});