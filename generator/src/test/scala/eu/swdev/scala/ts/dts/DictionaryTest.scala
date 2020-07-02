package eu.swdev.scala.ts.dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|

class DictionaryTest extends DtsFunSuite {

  """
    |export const dict1: { [key: string]: number }
    |export const dict2: { [key: string]: number | string }
    |""".check()

}

object DictionaryTest {
  @JSExportTopLevel("dict1")
  val dict1: js.Dictionary[Int] = ???
  @JSExportTopLevel("dict2")
  val dict2: js.Dictionary[Int | String] = ???
}
