package eu.swdev.scala.ts.dts

import java.net.URLClassLoader
import java.nio.file.{Path, Paths}

import eu.swdev.scala.ts.{Analyzer, Generator, SemSource}

import scala.meta.internal.semanticdb.Locator
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.meta.{Dialect, dialects}

trait DtsGeneration extends WithGlobalSymbolTable {

  def dialect: Dialect = dialects.Scala213

  def metaInfPath: Path = {

    val clazz = getClass
    val url   = clazz.getResource(s"${getClass.getSimpleName}.class")
    val uri   = url.toURI
    var res   = Paths.get(uri).getParent

    var s = clazz.getName
    while (s.indexOf('.') > 0) {
      s = s.substring(s.indexOf('.') + 1)
      res = res.getParent
    }

    res.resolve("META-INF")
  }

  /**
    * Generate the type declaration file for all Scala Meta text documents that contains symbol information for any
    * of the given classes.
    *
    * Note that not only the given classes are considered when generating the type declaration file but all
    * symbols that are defined in the corresponding files.
    */
  def dts(classes: Class[_]*): String = {

    val classSymbols = classes
      .map(_.getName.replace('.', '/'))
      .map { n =>
        if (n.endsWith("$")) {
          // symbol of a Scala object class
          s"${n.dropRight(1)}."
        } else {
          // symbol of a standard class
          s"$n#"
        }
      }
      .toSet

    val semSourcesBuilder = List.newBuilder[SemSource]

    Locator(metaInfPath) { (path, textDocuments) =>
      textDocuments.documents.foreach { td =>
        if (td.symbols.exists(si => classSymbols.contains(si.symbol))) {
          val semSrc = SemSource(td, dialect)
          semSourcesBuilder += semSrc
        }
      }
    }

    val semSources = semSourcesBuilder.result()

    def classLoaders(cl: ClassLoader): List[ClassLoader] =
        Option(cl).fold[List[ClassLoader]](Nil)(cl => cl :: classLoaders(cl.getParent))

    val urls = classLoaders(getClass.getClassLoader).collect {
      case ucl: URLClassLoader => ucl.getURLs
    }.flatten

    val paths       = urls.map(url => Paths.get(url.toURI).toAbsolutePath.toFile).map(AbsolutePath(_))
    val cp          = Classpath(paths)
    val symTab      = GlobalSymbolTable(cp, true)

    val exports = semSources.sortBy(_.td.uri).flatMap(Analyzer.analyze(_, symTab))

    Generator.generate(exports, symTab, Seq.empty, getClass.getClassLoader).trim
  }

}
