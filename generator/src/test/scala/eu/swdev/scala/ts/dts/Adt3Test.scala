package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class Adt3Test extends DtsFunSuite {

  """
    |export interface Adt3TestCase1$ extends eu.swdev.scala.ts.dts.Adt3Test.T {
    |  readonly str: string
    |  'Adt3TestCase1$': never
    |}
    |export const Adt3TestCase1: Adt3TestCase1$
    |export interface Adt3TestCase2$ extends eu.swdev.scala.ts.dts.Adt3Test.T {
    |  readonly num: number
    |  'Adt3TestCase2$': never
    |}
    |export const Adt3TestCase2: Adt3TestCase2$
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace Adt3Test {
    |            interface T {
    |              'eu.swdev.scala.ts.dts.Adt3Test.T': never
    |            }
    |            type T$u = Adt3TestCase1$ | Adt3TestCase2$
    |          }
    |        }
    |      }
    |    }
    |  }
    |}
    |""".check()
}

object Adt3Test {

  sealed trait T

  @JSExportTopLevel("Adt3TestCase1")
  @JSExportAll
  object Case1 extends T {
    val str = "abc"
  }
  @JSExportTopLevel("Adt3TestCase2")
  @JSExportAll
  object Case2 extends T {
    val num = 555
  }

}
