package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSGlobal, JSImport}

class ImportTest extends DtsFunSuite {

  test("dts") {
    """
      |import $random_ from 'random'
      |import * as $rxjs from 'rxjs'
      |export const observable: $rxjs.Observable<string>
      |export const random: $random_
      |""".check()
  }

}

object ImportTest {

  @JSImport("rxjs", "Observable")
  @js.native
  class Observable[T]

  @JSImport("random", JSImport.Default)
  @js.native
  object Random

  @JSExportTopLevel("observable")
  val observable: Observable[String] = ???

  @JSExportTopLevel("random")
  val random: Random.type = ???
}
