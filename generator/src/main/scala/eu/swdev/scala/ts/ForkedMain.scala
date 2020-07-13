package eu.swdev.scala.ts

import java.io.{File, FileOutputStream, FileWriter, OutputStreamWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.regex.Pattern

import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.matching.Regex

// this class is referenced in the ScalaTsPlugin when the generator is forked
class ForkedMain

/**
  * Main class that is started when the generator is forked by the ScalaTsPlugin
  */
object ForkedMain {

  case class Config(
      jsFile: File,
      moduleName: String,
      moduleVersion: String,
      considerFullCompileClasspath: Boolean,
      include: Pattern,
      exclude: Pattern,
      compileClassDir: File,
      compileFullClasspath: List[File],
      validate: Boolean,
  ) {
    import Config._

    def asEnvVars = Map(
      kJsFile                       -> jsFile.toString,
      kModuleName                   -> moduleName,
      kModuleVersion                -> moduleVersion,
      kConsiderFullCompileClassPath -> considerFullCompileClasspath.toString,
      kInclude                      -> include.pattern(),
      kExclude                      -> exclude.pattern(),
      kCompileClassDir              -> compileClassDir.toString,
      kCompileFullClassPath         -> compileFullClasspath.mkString(File.pathSeparator),
      kValidate                     -> String.valueOf(validate),
    )
  }

  object Config {

    val propertiesFileName = "scala-ts.properties"

    val kJsFile                       = "JS_FILE"
    val kModuleName                   = "MODULE_NAME"
    val kModuleVersion                = "MODULE_VERSION"
    val kConsiderFullCompileClassPath = "CONSIDER_FULL_COMPILE_CLASS_PATH"
    val kInclude                      = "INCLUDE"
    val kExclude                      = "EXCLUDE"
    val kCompileClassDir              = "COMPILE_CLASS_DIR"
    val kCompileFullClassPath         = "COMPILE_FULL_CLASS_PATH"
    val kValidate                     = "VALIDATE"

    def fromEnvVars = Config(
      jsFile = new File(System.getenv(kJsFile)),
      moduleName = System.getenv(kModuleName),
      moduleVersion = System.getenv(kModuleVersion),
      considerFullCompileClasspath = System.getenv(kConsiderFullCompileClassPath).toBoolean,
      include = Pattern.compile(System.getenv(kInclude)),
      exclude = Pattern.compile(System.getenv(kExclude)),
      compileClassDir = new File(System.getenv(kCompileClassDir)),
      compileFullClasspath = System.getenv(kCompileFullClassPath).split(File.pathSeparatorChar).map(new File(_)).toList,
      validate = System.getenv(kValidate).toBoolean,
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

    val semSrcs = if (config.considerFullCompileClasspath) {
      config.compileFullClasspath
        .filter(f => config.include.matcher(f.toString).find())
        .filter(f => !config.exclude.matcher(f.toString).find())
        .flatMap(SemSource.locate(_, dialect))
    } else {
      SemSource.locate(config.compileClassDir, dialect)
    }

    val inputs = semSrcs.flatMap(Analyzer.analyze(_, symTab))

    def inputInfo =
      inputs
        .groupBy(_.getClass.getSimpleName)
        .toList
        .sortBy(_._1)
        .map {
          case (cn, lst) => s"$cn: ${lst.length}"
        }
        .mkString("{", ", ", "}")

    System.out.println(s"ScalaTs input : $inputInfo")

    val output = Generator.generate(inputs, symTab, Seq.empty, getClass.getClassLoader)

    System.out.println(s"ScalaTs output: $dtsPath")

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

    if (config.validate) {
      val infos  = semSrcs.map(s => s -> DtsInfo(s)).collect { case (semSource, Some(dtsInfo)) => semSource -> dtsInfo }
      var failed = false
      def check(name: String, inputs: List[Input.Defn], expected: String): Unit = {
        val actual = Generator.generate(inputs, symTab, Seq.empty, getClass.getClassLoader).trim
        if (actual != expected) {
          System.err.println(s"$name: failed\n====== expected:\n$expected\n==== actual:\n$actual\n====")
          failed = true
        } else {
          System.out.println(s"$name: passed")
        }
      }
      infos.foreach {
        case (semSrc, DtsInfo.Dts(expectedDts)) =>
          val inputs = Analyzer.analyze(semSrc, symTab)
          check(s"file: ${semSrc.td.uri}", inputs, expectedDts)

        case (_, DtsInfo.DtsAndGroup(expectedDts, group)) =>
          val inputs = infos
            .collect {
              case (semSrc, DtsInfo.Group(`group`))          => semSrc
              case (semSrc, DtsInfo.DtsAndGroup(_, `group`)) => semSrc
            }
            .flatMap(Analyzer.analyze(_, symTab))
          check(s"group: $group", inputs, expectedDts)

        case _ =>
      }

      if (failed) {
        System.exit(1)
      }

    }

  }

}
