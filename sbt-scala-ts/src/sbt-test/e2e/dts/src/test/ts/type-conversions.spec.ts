import * as m from 'scala-ts-mod'

describe('type conversions in accessors', function() {

  const c = new m.TypeConversions()

  it('set / get array', function() {
    const mat = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]

    c.matrix = mat
    expect(c.matrix).toStrictEqual(mat)

    // c.matrix stored a copy of mat -> changing mat does not change c.matrix
    mat[0][0] = 0
    expect(c.matrix).not.toStrictEqual(mat)

    // c.matrix returns a copy -> c.matrix itself is not modified
    c.matrix[0][0] = 0
    expect(c.matrix).not.toStrictEqual(mat)

  });


});