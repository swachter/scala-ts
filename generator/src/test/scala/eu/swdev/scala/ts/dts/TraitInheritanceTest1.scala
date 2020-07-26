package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class TraitInheritanceTest1 extends DtsFunSuite {

  """
    |export interface ClassWithInheritedMethods1 extends eu.swdev.scala.ts.dts.TraitInheritanceTest1.Middle {
    |  'ClassWithInheritedMethods1': never
    |}
    |export class ClassWithInheritedMethods1 {
    |  constructor()
    |}
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace TraitInheritanceTest1 {
    |            interface Base {
    |              base(n: number): number
    |              'eu.swdev.scala.ts.dts.TraitInheritanceTest1.Base': never
    |            }
    |            interface Middle extends eu.swdev.scala.ts.dts.TraitInheritanceTest1.Base {
    |              middle(n: number): number
    |              'eu.swdev.scala.ts.dts.TraitInheritanceTest1.Middle': never
    |            }
    |          }
    |        }
    |      }
    |    }
    |  }
    |}
    |""".check()

}

object TraitInheritanceTest1 {
  @JSExportAll
  trait Base {
    def base(n: Int) = 2*n
  }

  @JSExportAll
  trait Middle extends Base {
    def middle(n: Int) = 3*n
  }

  @JSExportTopLevel("ClassWithInheritedMethods1")
  class Cls extends Middle
}
