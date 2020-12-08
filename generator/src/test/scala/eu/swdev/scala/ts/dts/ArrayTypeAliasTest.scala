package dts

import eu.swdev.scala.ts.DtsFunSuite
import eu.swdev.scala.ts.tpe.ReadOnlyArray

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

class ArrayTypeAliasTest extends DtsFunSuite {

  """
    |export class Example {
    |  constructor()
    |  foo(a: dts.Example.Elems): dts.Example.Elems
    |}
    |export namespace dts {
    |  namespace Example {
    |    type Elem = [string, any]
    |    type Elems = (dts.Example.Elem)[]
    |  }
    |}
    |""".check()

}

object Example {
  type Elem = js.Tuple2[String, js.Any]
  type Elems = js.Array[Elem]
}

@JSExportTopLevel("Example")
@JSExportAll
class Example() {

  import Example._

  def foo(a: Elems) = a

}


