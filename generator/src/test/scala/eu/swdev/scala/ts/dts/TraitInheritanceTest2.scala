package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class TraitInheritanceTest2 extends DtsFunSuite {

  """
    |export interface ClassWithInheritedMethods2 extends eu.swdev.scala.ts.dts.TraitInheritanceTest2.Middle {
    |  'ClassWithInheritedMethods2': never
    |}
    |export class ClassWithInheritedMethods2 {
    |  constructor()
    |  middle(n: number): number
    |  base(n: number): number
    |}
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace TraitInheritanceTest2 {
    |            interface Base {
    |              base(n: number): number
    |              'eu.swdev.scala.ts.dts.TraitInheritanceTest2.Base': never
    |            }
    |            interface Middle extends eu.swdev.scala.ts.dts.TraitInheritanceTest2.Base {
    |              middle(n: number): number
    |              'eu.swdev.scala.ts.dts.TraitInheritanceTest2.Middle': never
    |            }
    |          }
    |        }
    |      }
    |    }
    |  }
    |}
    |""".check()

}

object TraitInheritanceTest2 {
  trait Base extends js.Object {
    def base(n: Int): Int
  }

  trait Middle extends Base {
    def middle(n: Int): Int
  }

  @JSExportTopLevel("ClassWithInheritedMethods2")
  class Cls extends Middle {
    override def middle(n: Int): Int = 3*n

    override def base(n: Int): Int = 2*n
  }
}
