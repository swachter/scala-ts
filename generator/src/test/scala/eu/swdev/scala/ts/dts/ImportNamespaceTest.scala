package dts

import eu.swdev.scala.ts.DtsFunSuite

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport, JSName}

class ImportNamespaceTest extends DtsFunSuite {

  """
    |import * as $module from 'module'
    |export const namespaceImported: $module.ImportedClass
    |export const renamedNamespaceImported: $module.Renamed
    |""".check()

}


object ImportNamespaceTest extends js.Object {

  @JSImport("module", JSImport.Namespace)
  object ImportedNamespace extends js.Object {

    class ImportedClass

    @JSName("Renamed")
    class RenamedImportedClass

  }

  @JSExportTopLevel("namespaceImported")
  val namespaceImported: ImportedNamespace.ImportedClass = ???

  @JSExportTopLevel("renamedNamespaceImported")
  val renamedNamespaceImported: ImportedNamespace.RenamedImportedClass = ???
}


