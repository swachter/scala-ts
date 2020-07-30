package eu.swdev.scala.ts

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.regex.Pattern

import eu.swdev.scala.ts

import scala.meta.{Dialect, dialects}
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.control.NonFatal

// this class is referenced in the ScalaTsPlugin when the generator is forked
class AdapterGeneratorMain extends GeneratorMain

/**
  * Main class that is started when the TypeScript declaration file generator is forked by the ScalaTsPlugin
  */
object AdapterGeneratorMain extends AdapterGeneratorMain {

  case class Config(
      adapterFile: File,
      adapterName: String,
      include: Pattern,
      exclude: Pattern,
      compileFullClasspath: List[File],
      validate: Boolean,
  ) {
    import Config._

    def asEnvVars = Map(
      kAdapterFile          -> adapterFile.toString,
      kAdapterName          -> adapterName,
      kInclude              -> include.pattern(),
      kExclude              -> exclude.pattern(),
      kCompileFullClassPath -> compileFullClasspath.mkString(File.pathSeparator),
      kValidate             -> String.valueOf(validate),
    )
  }

  object Config {

    val kAdapterFile          = "ADAPTER_FILE"
    val kAdapterName          = "ADAPTER_NAME"
    val kInclude              = "INCLUDE"
    val kExclude              = "EXCLUDE"
    val kCompileFullClassPath = "COMPILE_FULL_CLASS_PATH"
    val kValidate             = "VALIDATE"

    def fromEnvVars = Config(
      adapterFile = new File(System.getenv(kAdapterFile)),
      adapterName = System.getenv(kAdapterName),
      include = Pattern.compile(System.getenv(kInclude)),
      exclude = Pattern.compile(System.getenv(kExclude)),
      compileFullClasspath = System.getenv(kCompileFullClassPath).split(File.pathSeparatorChar).map(new File(_)).toList,
      validate = System.getenv(kValidate).toBoolean,
    )

  }

  def main(args: Array[String]): Unit = {
    try {
      val config = Config.fromEnvVars
      val adapterPath = config.adapterFile.toPath

      val cp = config.compileFullClasspath.map(AbsolutePath(_))
      val symTab = GlobalSymbolTable(Classpath(cp), true)

      val semSrcs =
        config.compileFullClasspath
          .filter(f => config.include.matcher(f.toString).find())
          .filter(f => !config.exclude.matcher(f.toString).find())
          .flatMap(SemSource.locate(_, dialect))

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

      val output = AdapterGenerator.generate(inputs, symTab, config.adapterName)

      System.out.println(s"ScalaTs output: $adapterPath")

      Files.createDirectories(adapterPath.getParent)
      Files.write(adapterPath, output.getBytes(StandardCharsets.UTF_8))

      if (config.validate) {
        validate(semSrcs, symTab, inputs => AdapterGenerator.generate(inputs, symTab, config.adapterName))
      }

    } catch {
      case NonFatal(e) =>
        System.err.println("adapter creation failed")
        e.printStackTrace()
      System.exit(1)
    }

  }

}
