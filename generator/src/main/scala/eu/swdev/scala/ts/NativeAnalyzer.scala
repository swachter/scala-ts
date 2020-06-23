package eu.swdev.scala.ts

import scala.reflect.runtime.{universe => ru}
import ru._

class NativeAnalyzer(cl: ClassLoader) {

  val mirror = ru.runtimeMirror(cl)

  def nativeness(cls: Class[_]): Nativeness = {

    // depth first search for @js.native / @JSGlobal / @JSImport annotation
    // -> a @js.native annotation does not terminate the search
    def search(t: TypeSymbol, n: Nativeness): Nativeness = {
      val defaultName = t.name.toString

      def superClasses = t.toType.baseClasses.filter(_ != t).collect {
        case t: TypeSymbol => t
      }

      n match {
        case Nativeness.None =>
          val as = t.annotations
          (existsNativeAnnotation(as), findGlobalAnnotation(as, defaultName), findImportAnnotation(as, defaultName)) match {
            case (_, Some(n), _) => n
            case (_, _, Some(n)) => n
            case (true, _, _)    => superClasses.foldRight(Nativeness.marked)(search)
            case (false, _, _)   => superClasses.foldRight(Nativeness.none)(search)
          }

        case Nativeness.Marked =>
          val as = t.annotations
          (findGlobalAnnotation(as, defaultName), findImportAnnotation(as, defaultName)) match {
            case (Some(n), _) => n
            case (_, Some(n)) => n
            case (_, _)       => superClasses.foldRight(Nativeness.marked)(search)
          }

        case n: Nativeness.Global   => n
        case n: Nativeness.Imported => n
      }
    }
    val classSymbol = mirror.classSymbol(cls)
//    val as          = classSymbol.annotations
//    val ts          = as.map(_.tree)
//    val ns          = ts.map(_.tpe.toString)
//    val cs          = ts.map(_.children)

    search(classSymbol, Nativeness.None)
  }

  def isAnnotationOfType(t: String)(a: Annotation): Boolean = a.tree.tpe.toString == t

  def existsNativeAnnotation(as: List[Annotation]): Boolean = as.exists(isAnnotationOfType("scala.scalajs.js.native"))

  def findGlobalAnnotation(as: List[Annotation], defName: String): Option[Nativeness.Global] =
    as.collect {
      case a if isAnnotationOfType("scala.scalajs.js.annotation.JSGlobal")(a) =>
        val name = a.tree.children.tail match {
          case Literal(Constant(value)) :: Nil => String.valueOf(value)
          case _                               => defName
        }
        Nativeness.Global(name)
    }.headOption

  def findImportAnnotation(as: List[Annotation], defName: String): Option[Nativeness.Imported] =
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

  case object None                                  extends Nativeness
  case object Marked                                extends Nativeness
  case class Global(name: String)                   extends Nativeness
  case class Imported(module: String, name: String) extends Nativeness
}
