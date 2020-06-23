package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport}

class NativeAnalyzerTest extends AnyFunSuite with Matchers {

  val na = new NativeAnalyzer(getClass.getClassLoader)

  import NativeAnalyzerTest._

  test("global without name") {
    na.nativeness(classOf[GlobalWithoutName]) mustBe Nativeness.Global("GlobalWithoutName")
  }

  test("global with name") {
    na.nativeness(classOf[GlobalWithName]) mustBe Nativeness.Global("SomeName")
  }

  test("import with name") {
    na.nativeness(classOf[ImportWithName]) mustBe Nativeness.Imported("SomeModule", "SomeName")
  }

  test("import with default") {
    na.nativeness(classOf[ImportWithDefault]) mustBe Nativeness.Imported("SomeModule", "default")
  }

  test("import with namespace") {
    na.nativeness(classOf[ImportWithNamespace]) mustBe Nativeness.Marked
  }

}

object NativeAnalyzerTest {

  @JSGlobal
  @js.native
  class GlobalWithoutName

  @JSGlobal("SomeName")
  @js.native
  class GlobalWithName

  @JSImport("SomeModule", "SomeName")
  @js.native
  class ImportWithName

  @JSImport("SomeModule", JSImport.Default)
  @js.native
  class ImportWithDefault

  @JSImport("SomeModule", JSImport.Namespace)
  @js.native
  class ImportWithNamespace

}

