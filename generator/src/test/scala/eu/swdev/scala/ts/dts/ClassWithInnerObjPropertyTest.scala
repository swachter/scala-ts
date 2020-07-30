package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class ClassWithInnerObjPropertyTest extends DtsFunSuite {

  """
    |export interface AObj$ {
    |  readonly outerV1: AObj.OuterV1
    |  'AObj$': never
    |}
    |export const AObj: AObj$
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace AObj {
    |            interface OuterV1 {
    |              readonly Middle: eu.swdev.scala.ts.dts.AObj.OuterV1.Middle$
    |              'eu.swdev.scala.ts.dts.AObj.OuterV1': never
    |            }
    |            namespace OuterV1 {
    |              interface Middle$ {
    |                readonly newAdapter: eu.swdev.scala.ts.dts.AObj.OuterV1.MiddleV1
    |                'eu.swdev.scala.ts.dts.AObj.OuterV1.Middle$': never
    |              }
    |              interface MiddleV1 {
    |                'eu.swdev.scala.ts.dts.AObj.OuterV1.MiddleV1': never
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

@JSExportTopLevel("AObj")
object AObj extends js.Object {

  @JSExportAll
  trait OuterV1 {

    @JSExportAll
    trait MiddleV1

    object Middle extends js.Object {
      def newAdapter: MiddleV1 = ???
    }

    //  val middle = Middle
  }

  val outerV1: OuterV1 = ???
//  object OuterV1 extends js.Object {
//    def newAdapter: OuterV1 = ???
//  }

}

//@JSExportTopLevel("OuterClass")
//@JSExportAll
//class OuterClassWithInheritedObject extends OuterTrait

//@JSExportTopLevel("ClassWithInnerObjProperty")
//@JSExportAll
//class ClassWithInnerObjProperty {
//
//  trait InnerObj
//
//  object InnerObj extends js.Object {
//    val x = 1
//  }
//
//  val innnerObj = InnerObj
//}