package eu.swdev.scala.ts

import eu.swdev.scala.ts.Export.DefValVar

import scala.collection.mutable
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.transversers.Traverser
import scala.meta.{Defn, Init, Lit, Mod, Tree}
import scala.reflect.ClassTag
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

object Analyzer {

  def classSymbol[C](implicit ev: ClassTag[C]) = s"${ev.runtimeClass.getName.replace('.', '/')}#"

  val jsExportTopLevelSymbol = classSymbol[JSExportTopLevel]
  val jsExportSymbol         = classSymbol[JSExport]
  val jsExportAllSymbol      = classSymbol[JSExportAll]

  def analyze(semSrc: SemSource): List[Export] = {

    def existsSymbolReference(tree: Tree, symbol: String): Boolean =
      semSrc
        .symbolOccurrences(tree.pos, Role.REFERENCE)
        .exists(so => so.symbol == symbol)

    def existsTopLevelExportAnnot(tree: Tree): Boolean = existsSymbolReference(tree, jsExportTopLevelSymbol)
    def existsExportAnnot(tree: Tree): Boolean         = existsSymbolReference(tree, jsExportSymbol)
    def existsExportAllAnnot(tree: Tree): Boolean      = existsSymbolReference(tree, jsExportAllSymbol)

    def topLevelExportName(mods: List[Mod]): Option[SimpleName] = {
      mods.collect {
        case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsTopLevelExportAnnot(t) => SimpleName(lit)
      }.headOption
    }

    def exportName(mods: List[Mod], defaultName: String): Option[SimpleName] = {
      mods.collect {
        case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsExportAnnot(t) => SimpleName(lit)
        case Mod.Annot(t @ Init(_, _, _)) if existsExportAnnot(t)                           => SimpleName(defaultName)
      }.headOption
    }

    def exportAll(mods: List[Mod]): Boolean = {
      mods.exists {
        case Mod.Annot(t @ Init(_, _, _)) => existsExportAllAnnot(t)
        case _                            => false
      }
    }

    val traverser = new Traverser {

      val builder = List.newBuilder[Export]

      trait State {
        def process(tree: Tree, goOn: => Unit): Unit
      }

      object InitialState extends State {

        def processDefValVar[D <: Defn](defn: D, mods: List[Mod], ctor: (SemSource, D, SimpleName, SymbolInformation) => Export): Unit = {
          for {
            en <- topLevelExportName(mods)
            si <- semSrc.symbolInfo(defn.pos, Kind.METHOD)
          } {
            builder += ctor(semSrc, defn, en, si)
          }
        }

        def processContainer[D <: Defn](defn: D,
                                        mods: List[Mod],
                                        kind: Kind,
                                        ctor: (SemSource, D, SimpleName, SymbolInformation, List[Export.DefValVar]) => Export,
                                        visitChildrenIfNotExported: Boolean,
                                        visitChildren: => Unit): Unit = {
          for {
            en <- topLevelExportName(mods).orElse { if (visitChildrenIfNotExported) visitChildren; None }
            si <- semSrc.symbolInfo(defn.pos, kind)
          } {
            val memberBuilder = List.newBuilder[Export.DefValVar]
            state = InContainerState(memberBuilder, exportAll(mods))
            visitChildren
            state = InitialState
            builder += ctor(semSrc, defn, en, si, memberBuilder.result())
          }

        }

        override def process(tree: Tree, visitChildren: => Unit): Unit = tree match {
          case t @ Defn.Def(mods, _, _, _, _, _) => processDefValVar(t, mods, Export.Def)
          case t @ Defn.Val(mods, _, _, _)       => processDefValVar(t, mods, Export.Val)
          case t @ Defn.Var(mods, _, _, _)       => processDefValVar(t, mods, Export.Var)
          case t @ Defn.Class(mods, _, _, _, _)  => processContainer(t, mods, Kind.CLASS, Export.Cls, false, visitChildren)
          case t @ Defn.Object(mods, _, _)       => processContainer(t, mods, Kind.OBJECT, Export.Obj, true, visitChildren)
          case _                                 => visitChildren
        }
      }

      case class InContainerState(builder: mutable.Builder[Export.DefValVar, List[DefValVar]], exportAll: Boolean) extends State {

        def processDefValVar[D <: Defn](defn: D, mods: List[Mod], ctor: (SemSource, D, SimpleName, SymbolInformation) => Export.DefValVar): Unit = {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.METHOD)
            en <- exportName(mods, si.displayName).orElse(Some(SimpleName(si.displayName)).filter(_ => exportAll))
          } {
            builder += ctor(semSrc, defn, en, si)
          }
        }

        override def process(tree: Tree, goOn: => Unit): Unit = tree match {
          case t @ Defn.Def(mods, _, _, _, _, _) => processDefValVar(t, mods, Export.Def)
          case t @ Defn.Val(mods, _, _, _)       => processDefValVar(t, mods, Export.Val)
          case t @ Defn.Var(mods, _, _, _)       => processDefValVar(t, mods, Export.Var)
          case t @ Defn.Class(_, _, _, _, _)     => ()
          case t @ Defn.Object(_, _, _)          => ()
          case _                                 => goOn
        }

      }

      var state: State = InitialState

      override def apply(tree: Tree): Unit = {
        state.process(tree, super.apply(tree))
      }

    }

    traverser.apply(semSrc.source)

    traverser.builder.result()
  }

}
