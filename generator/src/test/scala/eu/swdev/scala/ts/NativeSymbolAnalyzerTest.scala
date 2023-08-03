package eu.swdev.scala.ts

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport, JSName}

class NativeSymbolAnalyzerTest extends AnyFunSuite with Matchers with ScalaMetaHelper {

  val na = new NativeSymbolAnalyzer(Map.empty, getClass.getClassLoader, symTab)

  def nativeSymbol(cls: Class[_]): Option[NativeSymbol] = {
    val sym = cls.getName.replace('.', '/').replace('$', '.') + "#"
    na.nativeSymbol(sym)
  }

  import NativeSymbolAnalyzerTest._

  test("global without name") {
    nativeSymbol(classOf[GlobalWithoutName]) mustBe Some(NativeSymbol.Global("GlobalWithoutName", true))
  }

  test("global with name") {
    nativeSymbol(classOf[GlobalWithName]) mustBe Some(NativeSymbol.Global("SomeName", false))
  }

  test("import with name") {
    nativeSymbol(classOf[ImportWithName]) mustBe Some(NativeSymbol.ImportedName("SomeModule", "SomeName", false))
  }

  test("import with default") {
    nativeSymbol(classOf[ImportWithDefault]) mustBe Some(NativeSymbol.ImportedName("SomeModule", "default", false))
  }

  test("import with namespace") {
    nativeSymbol(classOf[ImportWithNamespace]) mustBe Some(NativeSymbol.ImportedNamespace("SomeModule", false))
  }

  test("import with namespace / nested") {
    nativeSymbol(classOf[SomeModule.Nested]) mustBe Some(
      NativeSymbol.Inner("Nested", NativeSymbol.ImportedNamespace("SomeModule", true), true))
  }

  test("import with namespace / nested2 / nested3") {
    nativeSymbol(classOf[SomeModule2.Nested2.Nested3]) mustBe Some(
      NativeSymbol.Inner(
        "Nested3",
        NativeSymbol.Inner(
          "Nested2",
          NativeSymbol.ImportedNamespace(
            "SomeModule2",
            true
          ),
          true
        ),
        false
      )
    )
  }

}

object NativeSymbolAnalyzerTest {

  @JSGlobal
  @js.native
  class GlobalWithoutName extends js.Object

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

  @JSImport("SomeModule", JSImport.Namespace)
  object SomeModule extends js.Object {
    class Nested extends js.Object
  }

  @JSImport("SomeModule2", JSImport.Namespace)
  object SomeModule2 extends js.Object {
    object Nested2 extends js.Object {
      @JSName(js.Symbol.iterator)
      class Nested3
    }
  }
}
