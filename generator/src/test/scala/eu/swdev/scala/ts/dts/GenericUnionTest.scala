package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class GenericUnionTest extends DtsFunSuite {

  test("dts") {
    """
      |export interface GenUnionCase1<T extends object> extends eu.swdev.scala.ts.dts.GenericUnionTest.Base<T> {
      |  'GenUnionCase1': never
      |}
      |export class GenUnionCase1<T extends object> {
      |  constructor()
      |}
      |export interface GenUnionCase2<T extends object,X> extends eu.swdev.scala.ts.dts.GenericUnionTest.Base<T> {
      |  'GenUnionCase2': never
      |}
      |export class GenUnionCase2<T extends object,X> {
      |  constructor()
      |}
      |export interface GenUnionCase3<Y,T extends object> extends eu.swdev.scala.ts.dts.GenericUnionTest.Base<T> {
      |  'GenUnionCase3': never
      |}
      |export class GenUnionCase3<Y,T extends object> {
      |  constructor()
      |}
      |export namespace eu {
      |    namespace swdev {
      |        namespace scala {
      |            namespace ts {
      |                namespace dts {
      |                    namespace GenericUnionTest {
      |                        interface Base<T extends object> {
      |                          'eu.swdev.scala.ts.dts.GenericUnionTest.Base': never
      |                        }
      |                        type Base$u<T extends object,M1$X,M2$Y> = GenUnionCase1<T> | GenUnionCase2<T,M1$X> | GenUnionCase3<M2$Y,T>
      |                    }
      |                }
      |            }
      |        }
      |    }
      |}
      |""".check()
  }

}

object GenericUnionTest {

  sealed trait Base[T <: js.Object]

  @JSExportTopLevel("GenUnionCase1")
  class Case1[T <: js.Object] extends Base[T]
  @JSExportTopLevel("GenUnionCase2")
  class Case2[T <: js.Object, X] extends Base[T]
  @JSExportTopLevel("GenUnionCase3")
  class Case3[Y, T <: js.Object] extends Base[T]
}






