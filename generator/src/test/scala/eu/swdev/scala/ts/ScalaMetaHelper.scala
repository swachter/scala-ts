package eu.swdev.scala.ts

import java.net.URLClassLoader
import java.nio.file.{Path, Paths}

import scala.collection.mutable
import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}

trait ScalaMetaHelper {

  def dialect: Dialect = dialects.Scala

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

  val testDirPath: Path = {
    val clazz = getClass
    val url   = clazz.getResource(s"${getClass.getSimpleName}.class")
    val uri   = url.toURI
    Paths.get(uri).getParent
  }

  val basePath: Path = {
    val clazz = getClass
    var res   = testDirPath
    var s = clazz.getName
    while (s.indexOf('.') > 0) {
      s = s.substring(s.indexOf('.') + 1)
      res = res.getParent
    }
    res
  }

  val metaInfPath: Path = basePath.resolve("META-INF")

  def locateSemSources(dir: Path, dialect: Dialect) = ScalaMetaHelper.locateSemSources(dir, dialect)

}

object ScalaMetaHelper {

  def locateSemSources(dir: Path, dialect: Dialect): List[SemSource] = synchronized {
    map.getOrElseUpdate((dir, dialect), SemSource.locate(dir, dialect))
  }

  // caches located SemSources during test runs
  private val map = mutable.Map.empty[(Path, Dialect), List[SemSource]]
}
