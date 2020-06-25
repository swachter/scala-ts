package eu.swdev.scala.ts

import java.io.{File, FileOutputStream, FileWriter, OutputStreamWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}

// this trait is referenced in the ScalaTsPlugin when the generator is forked
trait ForkedMain

/**
 * Main class that is started when the generator is forked by the ScalaTsPlugin
 */
object ForkedMain {

  case class Config(
      jsFile: File,
      moduleName: String,
      moduleVersion: String,
      compileClassDir: File,
      compileFullClasspath: Seq[File]
  ) {
    import Config._

    def asEnvVars = Map(
      kJsFile               -> jsFile.toString,
      kModuleName           -> moduleName,
      kModuleVersion        -> moduleVersion,
      kCompileClassDir      -> compileClassDir.toString,
      kCompileFullClassPath -> compileFullClasspath.mkString(File.pathSeparator)
    )
  }

  object Config {

    val propertiesFileName = "scala-ts.properties"

    val kJsFile               = "JS_FILE"
    val kModuleName           = "MODULE_NAME"
    val kModuleVersion        = "MODULE_VERSION"
    val kCompileClassDir      = "COMPILE_CLASS_DIR"
    val kCompileFullClassPath = "COMPILE_FULL_CLASS_PATH"

    def fromEnvVars = Config(
      jsFile = new File(System.getenv(kJsFile)),
      moduleName = System.getenv(kModuleName),
      moduleVersion = System.getenv(kModuleVersion),
      compileClassDir = new File(System.getenv(kCompileClassDir)),
      compileFullClasspath = System.getenv(kCompileFullClassPath).split(File.pathSeparatorChar).map(new File(_)).toSeq
    )

  }

  private def dialect: Dialect = scala.util.Properties.versionNumberString match {
    case s if s.startsWith("2.12.") => dialects.Scala212
    case s if s.startsWith("2.13.") => dialects.Scala213
    case _                          => dialects.Scala
  }

  def main(args: Array[String]): Unit = {
    val config     = Config.fromEnvVars
    val jsPath     = config.jsFile.toPath
    val jsFileName = jsPath.getFileName.toString

    val idx               = jsFileName.lastIndexOf('.')
    val dtsFileNameString = s"${if (idx >= 0) jsFileName.substring(0, idx) else jsFileName}.d.ts"
    val dtsPath           = jsPath.resolveSibling(dtsFileNameString)

    val cp     = config.compileFullClasspath.map(AbsolutePath(_)).toList
    val symTab = GlobalSymbolTable(Classpath(cp), true)

    val semSrcs = SemSource.from(config.compileClassDir, dialect)

    val inputs = semSrcs.sortBy(_.td.uri).flatMap(Analyzer.analyze(_, symTab))

    def inputInfo =
      inputs
        .groupBy(_.getClass.getSimpleName)
        .toList
        .sortBy(_._1)
        .map {
          case (cn, lst) => s"$cn: ${lst.length}"
        }
        .mkString("{", ", ", "}")

    println(s"ScalaTs input : $inputInfo")

    val output = Generator.generate(inputs, symTab, Seq.empty, getClass.getClassLoader)

    println(s"ScalaTs output: $dtsPath")

    Files.write(dtsPath, output.getBytes(StandardCharsets.UTF_8))

    val packageJsonPath = jsPath.resolveSibling("package.json")

    val content =
      s"""
         |{
         |  "name": "${config.moduleName}",
         |  "main": "$jsFileName",
         |  "version": "${config.moduleVersion}",
         |  "type": "module"
         |}
         |""".stripMargin

    Files.write(packageJsonPath, content.getBytes(StandardCharsets.UTF_8))

  }

}
