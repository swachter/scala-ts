package eu.swdev.scala.ts.dts

import java.net.URLClassLoader
import java.nio.file.Paths

import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}

trait WithGlobalSymbolTable {

  val symTab = {
    def classLoaders(cl: ClassLoader): List[ClassLoader] =
      Option(cl).fold[List[ClassLoader]](Nil)(cl => cl :: classLoaders(cl.getParent))
    val urls = classLoaders(getClass.getClassLoader).collect {
      case ucl: URLClassLoader => ucl.getURLs
    }.flatten

    val paths = urls.map(url => Paths.get(url.toURI).toAbsolutePath.toFile).map(AbsolutePath(_))
    val cp    = Classpath(paths)
    GlobalSymbolTable(cp, true)
  }

}
