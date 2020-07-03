package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class ObjectTest extends DtsFunSuite {

  """
    |export interface SomeObject$ {
    |  doIt(): void
    |  readonly x: number
    |  y: number
    |  readonly o: eu.swdev.scala.ts.dts.ObjectTest$.o$
    |  'SomeObject$': never
    |}
    |export const SomeObject: SomeObject$
    |export namespace eu {
    |  namespace swdev {
    |    namespace scala {
    |      namespace ts {
    |        namespace dts {
    |          namespace ObjectTest$ {
    |            interface o$ {
    |              readonly a: string
    |              'eu.swdev.scala.ts.dts.ObjectTest$.o$': never
    |            }
    |          }
    |        }
    |      }
    |    }
    |  }
    |}
    |""".check()

}

@JSExportTopLevel("SomeObject")
@JSExportAll
object ObjectTest {

  def doIt() = ()

  val x = 1
  var y = 2

  @JSExportAll
  object o {
    val a = "abc"
  }

  private val a = 0
  private var b = 1
  private def m = ()

}