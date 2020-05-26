package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.internal.semanticdb.{MethodSignature, SymbolInformation, ValueSignature}
import scala.meta.internal.{semanticdb => isb}
import scala.meta.transversers.Traverser
import scala.meta.{Defn, Init, Lit, Mod, Term, Tree}
import scala.reflect.ClassTag
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

object Analyzer {

  def classSymbol[C](implicit ev: ClassTag[C]) = s"${ev.runtimeClass.getName.replace('.', '/')}#"

  val jsExportTopLevelSymbol = classSymbol[JSExportTopLevel]
  val jsExportSymbol         = classSymbol[JSExport]
  val jsExportAllSymbol      = classSymbol[JSExportAll]

  def analyze(semSrc: SemSource): List[Export.TopLevel] = {

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

    def hasCaseClassMod(mods: List[Mod]): Boolean = mods.exists(_.isInstanceOf[Mod.Case])
    def hasValMod(mods: List[Mod]): Boolean       = mods.exists(_.isInstanceOf[Mod.ValParam])
    def hasVarMod(mods: List[Mod]): Boolean       = mods.exists(_.isInstanceOf[Mod.VarParam])
    def hasPrivateMod(mods: List[Mod]): Boolean   = mods.exists(_.isInstanceOf[Mod.Private])

    val traverser = new Traverser {

      val builder = List.newBuilder[Export.TopLevel]

      trait State {
        def process(tree: Tree, visitChildren: => Unit): Unit
      }

      object InitialState extends State {

        def processDefValVar[D <: Defn](defn: D,
                                        mods: List[Mod],
                                        ctor: (SemSource, D, SimpleName, SymbolInformation) => Export.TopLevel): Unit = {
          for {
            en <- topLevelExportName(mods)
            si <- semSrc.symbolInfo(defn.pos, Kind.METHOD)
          } {
            builder += ctor(semSrc, defn, en, si)
          }
        }

        def processCls(defn: Defn.Class, mods: List[Mod], visitChildren: => Unit): Unit = {
          for {
            en <- topLevelExportName(mods)
            si <- semSrc.symbolInfo(defn.pos, Kind.CLASS)
          } {
            val memberBuilder = List.newBuilder[Export.Member]

            val ctorParamTerms = defn.ctor.paramss.flatten

            val isCaseClass = hasCaseClassMod(mods)

            val ctorParams = semSrc.symbolInfo(defn.pos, Kind.CONSTRUCTOR) match {
              case Some(ctorSi) =>
                val ctorSig = ctorSi.signature.asInstanceOf[MethodSignature]
                val ctorParamSymbolInfos = ctorSig.parameterLists
                  .flatMap(_.symlinks.map(semSrc.symbolInfo(_)))

                ctorParamSymbolInfos.flatMap { ctorParamSymbolInfo =>
                  ctorParamTerms.collect {
                    case tp @ Term.Param(termMods, termName, termType, defaultTerm)
                        if termName.value == ctorParamSymbolInfo.displayName && !hasPrivateMod(termMods) && hasVarMod(termMods) =>
                      Export.CtorParam(semSrc,
                                       tp,
                                       SimpleName(ctorParamSymbolInfo.displayName),
                                       ctorParamSymbolInfo,
                                       Export.CtorParamMod.Var)
                    case tp @ Term.Param(termMods, termName, termType, defaultTerm)
                        if termName.value == ctorParamSymbolInfo.displayName && !hasPrivateMod(termMods) && (isCaseClass || hasValMod(
                          termMods)) =>
                      Export.CtorParam(semSrc,
                                       tp,
                                       SimpleName(ctorParamSymbolInfo.displayName),
                                       ctorParamSymbolInfo,
                                       Export.CtorParamMod.Val)
                    case tp @ Term.Param(termMods, termName, termType, defaultTerm) if termName.value == ctorParamSymbolInfo.displayName =>
                      Export.CtorParam(semSrc,
                                       tp,
                                       SimpleName(ctorParamSymbolInfo.displayName),
                                       ctorParamSymbolInfo,
                                       Export.CtorParamMod.Loc)
                  }
                }.toList
              case None => Nil
            }

            state = InContainerState(memberBuilder, exportAll(mods))
            visitChildren
            state = InitialState
            builder += Export.Cls(semSrc, defn, en, si, memberBuilder.result(), ctorParams)
          }

        }

        def processObj(defn: Defn.Object, mods: List[Mod], visitChildren: => Unit): Unit = {
          for {
            en <- topLevelExportName(mods).orElse { visitChildren; None }
            si <- semSrc.symbolInfo(defn.pos, Kind.OBJECT)
          } {
            val memberBuilder = List.newBuilder[Export.Member]
            state = InContainerState(memberBuilder, exportAll(mods))
            visitChildren
            state = InitialState
            builder += Export.Obj(semSrc, defn, en, si, memberBuilder.result())
          }

        }

        override def process(tree: Tree, visitChildren: => Unit): Unit = tree match {
          case t @ Defn.Def(mods, _, _, _, _, _) => processDefValVar(t, mods, Export.Def)
          case t @ Defn.Val(mods, _, _, _)       => processDefValVar(t, mods, Export.Val)
          case t @ Defn.Var(mods, _, _, _)       => processDefValVar(t, mods, Export.Var)
          case t @ Defn.Class(mods, _, _, _, _)  => processCls(t, mods, visitChildren)
          case t @ Defn.Object(mods, _, _)       => processObj(t, mods, visitChildren)
          case _                                 => visitChildren
        }
      }

      case class InContainerState(builder: mutable.Builder[Export.Member, List[Export.Member]], exportAll: Boolean) extends State {

        def processDefValVar[D <: Defn](defn: D,
                                        mods: List[Mod],
                                        ctor: (SemSource, D, SimpleName, SymbolInformation) => Export.Member): Unit = {
          if (!hasPrivateMod(mods)) {
            for {
              si <- semSrc.symbolInfo(defn.pos, Kind.METHOD)
              en <- exportName(mods, si.displayName).orElse(Some(SimpleName(si.displayName)).filter(_ => exportAll))
            } {
              builder += ctor(semSrc, defn, en, si)
            }
          }
        }

        override def process(tree: Tree, visitChildren: => Unit): Unit = tree match {
          case t @ Defn.Def(mods, _, _, _, _, _) => processDefValVar(t, mods, Export.Def)
          case t @ Defn.Val(mods, _, _, _)       => processDefValVar(t, mods, Export.Val)
          case t @ Defn.Var(mods, _, _, _)       => processDefValVar(t, mods, Export.Var)
          case t @ Defn.Class(_, _, _, _, _)     => ()
          case t @ Defn.Object(_, _, _)          => ()
          case _                                 => visitChildren
        }

      }

      var state: State = InitialState

      override def apply(tree: Tree): Unit = {
        state.process(tree, super.apply(tree))
      }

    }

    traverser.apply(semSrc.source)

    val exports = traverser.builder.result()
    exports
  }

  // determine all types that are referenced in the given export item
  def referencedTypes(e: Export): List[isb.Type] = e match {
    case e: Export.Def       => e.methodSignature.returnType :: parameterTypes(e)
    case e: Export.Val       => List(e.methodSignature.returnType)
    case e: Export.Var       => List(e.methodSignature.returnType)
    case e: Export.Cls       => e.member.flatMap(referencedTypes) ++ e.ctorParams.flatMap(referencedTypes)
    case e: Export.Obj       => e.member.flatMap(referencedTypes)
    case e: Export.CtorParam => List(e.valueSignature.tpe)
  }

  def parameterTypes(e: Export.Def): List[isb.Type] = {
    def argType(symbol: String): isb.Type = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      vs.tpe
    }
    e.methodSignature.parameterLists.flatMap(_.symlinks).map(argType).toList
  }

}
