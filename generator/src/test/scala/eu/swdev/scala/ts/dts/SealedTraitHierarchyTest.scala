package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class SealedTraitHierarchyTest extends DtsFunSuite {

  """
    |export interface Case1<X> extends dts.SealedTraitHierarchyTest.Base<X> {
    |  'Case1': never
    |}
    |export class Case1<X> {
    |  constructor()
    |}
    |export interface Case2<X,Y> extends dts.SealedTraitHierarchyTest.Base<X> {
    |  'Case2': never
    |}
    |export class Case2<X,Y> {
    |  constructor()
    |}
    |export interface Case3<X,Y> extends dts.SealedTraitHierarchyTest.Middle<X,Y> {
    |  'Case3': never
    |}
    |export class Case3<X,Y> {
    |  constructor()
    |}
    |export interface Case4<X,Y,Z> extends dts.SealedTraitHierarchyTest.Middle<X,Y> {
    |  'Case4': never
    |}
    |export class Case4<X,Y,Z> {
    |  constructor()
    |}
    |export interface Case5<X,Y,Z> extends dts.SealedTraitHierarchyTest.Middle<X,Y> {
    |  'Case5': never
    |}
    |export class Case5<X,Y,Z> {
    |  constructor()
    |}
    |export namespace dts {
    |  namespace SealedTraitHierarchyTest {
    |    interface Base<X> {
    |      'dts.SealedTraitHierarchyTest.Base': never
    |    }
    |    type Base$u<X,$M1_Y,$M2_Y,$M2_1_Z,$M2_2_Z> = Case1<X> | Case2<X,$M1_Y> | dts.SealedTraitHierarchyTest.Middle$u<X,$M2_Y,$M2_1_Z,$M2_2_Z>
    |    interface Middle<X,Y> extends dts.SealedTraitHierarchyTest.Base<X> {
    |      'dts.SealedTraitHierarchyTest.Middle': never
    |    }
    |    type Middle$u<X,Y,$M1_Z,$M2_Z> = Case3<X,Y> | Case4<X,Y,$M1_Z> | Case5<X,Y,$M2_Z>
    |  }
    |}
    |""".check()

}

object SealedTraitHierarchyTest {

  sealed trait Base[X]

  sealed trait Middle[X, Y] extends Base[X]

  @JSExportTopLevel("Case1")
  class Case1[X] extends Base[X]

  @JSExportTopLevel("Case2")
  class Case2[X, Y] extends Base[X]

  @JSExportTopLevel("Case3")
  class Case3[X, Y] extends Middle[X, Y]

  @JSExportTopLevel("Case4")
  class Case4[X, Y, Z] extends Middle[X, Y]

  @JSExportTopLevel("Case5")
  class Case5[X, Y, Z] extends Middle[X, Y]

}










