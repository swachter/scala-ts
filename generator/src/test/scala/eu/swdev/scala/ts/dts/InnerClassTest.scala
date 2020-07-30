package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js.annotation.JSExportTopLevel

class InnerClassTest extends DtsFunSuite {

  """
    |export const test: eu.swdev.scala.ts.dts.InnerClassTest.Outer.Inner
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace InnerClassTest {
    |            namespace Outer {
    |              interface Inner {
    |                'eu.swdev.scala.ts.dts.InnerClassTest.Outer.Inner': never
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

object InnerClassTest {

  @JSExportTopLevel("test")
  val test: Outer#Inner = ???

  class Outer {
    class Inner
  }

}