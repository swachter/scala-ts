package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

class OuterTest extends DtsFunSuite {

  """
    |export class OuterClass {
    |  constructor()
    |  readonly midd: eu.swdev.scala.ts.dts.Outer.midd$
    |}
    |export interface OuterObject$ {
    |  readonly middle: eu.swdev.scala.ts.dts.Outer.middle$
    |  'OuterObject$': never
    |}
    |export const OuterObject: OuterObject$
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace Outer {
    |            interface midd$ {
    |              x: number
    |              'eu.swdev.scala.ts.dts.Outer.midd$': never
    |            }
    |            interface middle$ {
    |              readonly innerMost: eu.swdev.scala.ts.dts.Outer.middle.innerMost$
    |              'eu.swdev.scala.ts.dts.Outer.middle$': never
    |            }
    |            namespace middle {
    |              interface innerMost$ {
    |                x: number
    |                'eu.swdev.scala.ts.dts.Outer.middle.innerMost$': never
    |              }
    |            }
    |          }
    |        }
    |      }
    |    }
    |  }
    |}
    |""".check()

}


@JSExportTopLevel("OuterClass")
class Outer extends js.Object {
  object midd extends js.Object {
    //    object innerMost extends js.Object {
    var x = 1
    //    }
  }
}

@JSExportTopLevel("OuterObject")
object Outer extends js.Object {
  object middle extends js.Object {
    object innerMost extends js.Object {
      var x = 1
    }
  }
}


