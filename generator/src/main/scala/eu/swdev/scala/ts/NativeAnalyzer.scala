package eu.swdev.scala.ts

import scala.reflect.runtime.{universe => ru}
import ru._

class NativeAnalyzer(cl: ClassLoader) {

  val mirror = ru.runtimeMirror(cl)

  def nativeness(fn: FullName): Nativeness = {
    if (fn.str.endsWith("$")) {
      val ms = mirror.staticModule(fn.str.dropRight(1))
      nativeness(ms.moduleClass.asClass)
    } else {
      val cs = mirror.staticClass(fn.str)
      nativeness(cs)
    }
  }

  def nativeness(cls: Class[_]): Nativeness = {
    val classSymbol = mirror.classSymbol(cls)
    nativeness(classSymbol)
  }

  private def nativeness(t: TypeSymbolApi): Nativeness = {
    val defaultName = t.name.toString
    val as          = t.annotations
    (findGlobalAnnot(as, defaultName), findImportAnnot(as, defaultName)) match {
      case (Some(n), _) => n
      case (_, Some(n)) => n
      case _ if hasJsNativeAnnot(t) => Nativeness.Marked
      case _ => Nativeness.None
    }

  }

  // depth first search for @js.native annotation
  private def hasJsNativeAnnot(t: TypeSymbolApi): Boolean = {
    def superClasses = t.toType.baseClasses.filter(_ != t).collect {
      case t: TypeSymbol => t
    }
    existsJsNativeAnnot(t.annotations) || superClasses.exists(hasJsNativeAnnot)
  }

  def isAnnotationOfType(t: String)(a: Annotation): Boolean = a.tree.tpe.toString == t

  def existsJsNativeAnnot(as: List[Annotation]): Boolean = as.exists(isAnnotationOfType("scala.scalajs.js.native"))

  def findGlobalAnnot(as: List[Annotation], defName: String): Option[Nativeness.Global] =
    as.collect {
      case a if isAnnotationOfType("scala.scalajs.js.annotation.JSGlobal")(a) =>
        val name = a.tree.children.tail match {
          case Literal(Constant(value)) :: Nil => String.valueOf(value)
          case _                               => defName
        }
        Nativeness.Global(name)
    }.headOption

  def findImportAnnot(as: List[Annotation], defName: String): Option[Nativeness.Imported] =
    as.collect {
        case a if isAnnotationOfType("scala.scalajs.js.annotation.JSImport")(a) =>
          (a.tree.children.tail match {
            case Literal(Constant(value1)) :: Literal(Constant(value2)) :: _ => Some((String.valueOf(value1), String.valueOf(value2)))
            case _                                                           => None
          }).map(Nativeness.Imported.tupled)
      }
      .collect {
        case Some(s) => s
      }
      .headOption

}

sealed trait Nativeness extends Product with Serializable

object Nativeness {

  val marked: Nativeness = Marked
  val none: Nativeness   = None

  sealed trait Named extends Nativeness

  case object None                                  extends Nativeness
  case object Marked                                extends Nativeness
  case class Global(name: String)                   extends Named
  case class Imported(module: String, name: String) extends Named
}
