import * as m from 'from-scala'

describe('array', function() {

  it('set / get vector', function() {
    const vec = [1, 2, 3]
    const mat = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    const c = new m.ArrayAccess(vec, mat)

    expect(c.vector.length).toBe(vec.length)
    expect(c.matrix.length).toBe(mat.length)
    expect(c.matrix[0].length).toBe(mat[0].length)

    expect(c.vector).toStrictEqual(vec)
    expect(c.matrix).toStrictEqual(mat)

    const vec2 = [4]
    const mat2 = [[1]]
    c.vector = vec2
    c.matrix = mat2
    expect(c.vector).toStrictEqual(vec2)
    expect(c.matrix).toStrictEqual(mat2)
  });

});