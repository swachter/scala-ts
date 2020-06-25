package eu.swdev.scala.ts.dts

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSGlobal}

class GlobalTest extends DtsFunSuite {

  test("dts") {
    """
      |export const weakMap: WeakMap<string,string>
      |export const weakSet: WeakSet<string>
      |""".check()
  }

}

object GlobalTest {

  @JSGlobal
  @js.native
  class WeakMap[K, V]

  @JSGlobal("WeakSet")
  @js.native
  class WeakSetRenamed[T]

  @JSExportTopLevel("weakMap")
  val map: WeakMap[String, String] = ???

  @JSExportTopLevel("weakSet")
  val set: WeakSetRenamed[String] = ???
}



