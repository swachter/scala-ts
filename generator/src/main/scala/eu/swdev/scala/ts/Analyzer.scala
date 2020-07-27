package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.internal.semanticdb.{MethodSignature, SymbolInformation}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.transversers.Traverser
import scala.meta.{Decl, Defn, Init, Lit, Mod, Name, Stat, Term, Tree}
import scala.reflect.ClassTag
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import eu.swdev.scala.ts.annotation.{Adapt, AdaptMembers, AdaptConstructor, AdaptAll}

object Analyzer {

  def classSymbol[C](implicit ev: ClassTag[C]) = s"${ev.runtimeClass.getName.replace('.', '/')}#"

  val jsExportTopLevelSymbol   = classSymbol[JSExportTopLevel]
  val jsExportSymbol           = classSymbol[JSExport]
  val jsExportAllSymbol        = classSymbol[JSExportAll]
  val jsExportStaticSymbol     = "scala/scalajs/js/annotation/JSExportStatic#"
  val jsNameSymbol             = "scala/scalajs/js/annotation/JSName#"
  val jsAdaptSymbol            = classSymbol[Adapt]
  val jsAdaptMembersSymbol     = classSymbol[AdaptMembers]
  val jsAdaptConstructorSymbol = classSymbol[AdaptConstructor]
  val jsAdaptAllSymbol         = classSymbol[AdaptAll]

  /**
    * @return flattened list of all input definitions (nested input definitions are included in the list)
    */
  def analyze(semSrc: SemSource, symTab: SymbolTable): List[Input.Defn] = {

    def existsSymbolReference(tree: Tree, symbol: String): Boolean =
      semSrc
        .symbolOccurrences(tree.pos, Role.REFERENCE)
        .exists(so => so.symbol == symbol)

    def existsTopLevelExportAnnot(tree: Tree): Boolean = existsSymbolReference(tree, jsExportTopLevelSymbol)
    def existsExportAnnot(tree: Tree): Boolean         = existsSymbolReference(tree, jsExportSymbol)
    def existsExportAllAnnot(tree: Tree): Boolean      = existsSymbolReference(tree, jsExportAllSymbol)
    def existsExportStaticAnnot(tree: Tree): Boolean   = existsSymbolReference(tree, jsExportStaticSymbol)
    def existsJsNameAnnot(tree: Tree): Boolean         = existsSymbolReference(tree, jsNameSymbol)

    def existsAdaptAnnot(tree: Tree): Boolean            = existsSymbolReference(tree, jsAdaptSymbol)
    def existsAdaptMembersAnnot(tree: Tree): Boolean     = existsSymbolReference(tree, jsAdaptMembersSymbol)
    def existsAdaptConstructorAnnot(tree: Tree): Boolean = existsSymbolReference(tree, jsAdaptConstructorSymbol)
    def existsAdaptAllAnnot(tree: Tree): Boolean         = existsSymbolReference(tree, jsAdaptAllSymbol)

    def referencedSymbol(term: Term): Symbol = {
      // the term must reference a static, stable field (cf. https://www.scala-js.org/doc/interoperability/facade-types.html)
      // of type js.Symbol
      // -> find the symbol occurrence in the range of the given term
      // -> there might be several indexes; e.g.: `js`, `js.Symbol`, and `js.Symbol.iterator`
      // -> use the symbol occurrence with the greatest endCharacter index
      val seq = semSrc.symbolOccurrences(term.pos, Role.REFERENCE)
      seq.sortBy(_.range.get.endCharacter).last.symbol
    }

    val topLevelVisibilityAnnot: PartialFunction[Mod, Visibility.TopLevel] = {
      case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsTopLevelExportAnnot(t) => Visibility.TopLevel(lit)
    }

    val memberVisibiltyAnnot: PartialFunction[Mod, Visibility.Member] = {
      case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsExportAnnot(t) => Visibility.JSExportWithName(lit)
      case Mod.Annot(t @ Init(_, _, Nil)) if existsExportAnnot(t)                         => Visibility.JSExportWithoutName
      case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsJsNameAnnot(t) => Visibility.JsNameWithString(lit)
      case Mod.Annot(t @ Init(_, _, List(List(ref)))) if existsJsNameAnnot(t)             => Visibility.JsNameWithSymbol(referencedSymbol(ref))
    }

    val staticVisibilityAnnot: PartialFunction[Mod, Visibility.Static] = {
      case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsExportStaticAnnot(t) => Visibility.JSExportStaticWithName(lit)
      case Mod.Annot(t @ Init(_, _, Nil)) if existsExportStaticAnnot(t)                         => Visibility.JSExportStaticWithoutName
    }

    val visibilityAnnot = topLevelVisibilityAnnot orElse memberVisibiltyAnnot orElse staticVisibilityAnnot

    val adaptAnnot: PartialFunction[Mod, Adapted] = {
      case Mod.Annot(t @ Init(_, _, List(List(Lit.String(lit))))) if existsAdaptAnnot(t) => Adapted.WithOverriddenInteropType(lit)
      case Mod.Annot(t @ Init(_, _, Nil)) if existsAdaptAnnot(t)                         => Adapted.WithDefaultInteropType
    }

    def visibility(mods: List[Mod]): Option[Visibility] = mods.collect(visibilityAnnot).headOption
    def adapted(mods: List[Mod]): Option[Adapted]       = mods.collect(adaptAnnot).headOption

    def fieldName(mods: List[Mod], default: String): String =
      mods
        .collect(memberVisibiltyAnnot)
        .map {
          case Visibility.DisplayName         => default
          case Visibility.JSExportWithoutName => default
          case Visibility.JSExportWithName(s) => s
          case Visibility.JsNameWithString(s) => s
          case Visibility.JsNameWithSymbol(s) =>
            throw new NotImplementedError("constructor parameters can not be annotated with @JSName with a symbol")
        }
        .headOption
        .getOrElse(default)

    def hasAnnot(mods: List[Mod], pred: Tree => Boolean): Boolean = mods.exists {
      case Mod.Annot(t @ Init(_, _, _)) => pred(t)
      case _                            => false
    }

    def hasExportAllAnnot(mods: List[Mod]): Boolean = hasAnnot(mods, existsExportAllAnnot)

    def hasAdaptMembersAnnot(mods: List[Mod]): Boolean     = hasAnnot(mods, existsAdaptMembersAnnot)
    def hasAdaptConstructorAnnot(mods: List[Mod]): Boolean = hasAnnot(mods, existsAdaptConstructorAnnot)
    def hasAdaptAllAnnot(mods: List[Mod]): Boolean         = hasAnnot(mods, existsAdaptAllAnnot)

    def areAllMembersVisible(mods: List[Mod], si: SymbolInformation): Boolean = hasExportAllAnnot(mods) || isSubtypeOfJsAny(si)
    def areAllMembersAdapted(mods: List[Mod]): Boolean                        = hasAdaptAllAnnot(mods) || hasAdaptMembersAnnot(mods)

    def hasCaseClassMod(mods: List[Mod]): Boolean = mods.exists(_.isInstanceOf[Mod.Case])
    def hasValMod(mods: List[Mod]): Boolean       = mods.exists(_.isInstanceOf[Mod.ValParam])
    def hasVarMod(mods: List[Mod]): Boolean       = mods.exists(_.isInstanceOf[Mod.VarParam])
    def hasPrivateMod(mods: List[Mod]): Boolean   = mods.exists(_.isInstanceOf[Mod.Private])
    def hasAbstractMod(mods: List[Mod]): Boolean  = mods.exists(_.isInstanceOf[Mod.Abstract])

    def isSubtypeOfJsAny(si: SymbolInformation): Boolean = si.isSubtypeOf("scala/scalajs/js/Any#", symTab)

    /**
      * Recursively traverses source trees and collects input definitions.
      *
      * The traversal state is managed by a stack of State instances. For each container (i.e. class, object, or trait)
      * a nested state is pushed on the stack. State instances build lists of input definitions. The initial state
      * collects top level definitions whereas nested states collect container members.
      */
    val traverser = new Traverser {

      class State(allStateMembersAreVisible: Boolean, allStateMembersAreAdapted: Boolean) {

        val builder = List.newBuilder[Input.Defn]

        val inheritedVisiblity                   = if (allStateMembersAreVisible) Visibility.DisplayName else Visibility.No
        val inheritedAdapted                     = if (allStateMembersAreAdapted) Adapted.WithDefaultInteropType else Adapted.No
        def effectiveVisibility(mods: List[Mod]) = visibility(mods).getOrElse(inheritedVisiblity)
        def effectiveAdapted(mods: List[Mod])    = adapted(mods).getOrElse(inheritedAdapted)

        def processDefValVar[D <: Stat](defn: D,
                                        mods: List[Mod],
                                        isAbstract: Boolean,
                                        ctor: (SemSource, D, Visibility, Adapted, SymbolInformation, Boolean) => Input.Defn): Unit =
          if (!hasPrivateMod(mods)) {
            for {
              si <- semSrc.symbolInfo(defn.pos, Kind.METHOD)
            } {
              builder += ctor(semSrc, defn, effectiveVisibility(mods), effectiveAdapted(mods), si, isAbstract)
            }
          }

        def recurse(allMembersAreVisible: Boolean, allMembersAreAdapted: Boolean, visitChildren: => Unit): List[Input.Defn] = {
          states.push(new State(allMembersAreVisible, allMembersAreAdapted))
          visitChildren
          states.pop().builder.result()
        }

        def processCls(defn: Defn.Class, visitChildren: => Unit): Unit = if (!hasPrivateMod(defn.mods)) {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.CLASS)
          } {
            val ctorParamTerms = defn.ctor.paramss.flatten

            val isCaseClass          = hasCaseClassMod(defn.mods)
            val allMembersAreVisible = areAllMembersVisible(defn.mods, si)
            val allMembersAreAdapted = areAllMembersAdapted(defn.mods)

            val ctorParams = semSrc.symbolInfo(defn.pos, Kind.CONSTRUCTOR) match {
              case Some(ctorSi) =>
                val ctorSig              = ctorSi.signature.asInstanceOf[MethodSignature]
                val ctorParamSymbolInfos = ctorSig.parameterLists.flatMap(_.symlinks.map(semSrc.symbolInfo(_)))
                ctorParamSymbolInfos.flatMap { ctorParamSi =>
                  ctorParamTerms
                    .collect {
                      case tp @ Term.Param(_, Name(ctorParamSi.displayName), _, _) => tp
                    }
                    .headOption
                    .map { termParam =>
                      def fldName = fieldName(termParam.mods, termParam.name.value)
                      val m = termParam.mods match {
                        case mods if hasPrivateMod(mods)            => Input.CtorParamMod.Prv
                        case mods if hasVarMod(mods)                => Input.CtorParamMod.Var(fldName)
                        case mods if hasValMod(mods) || isCaseClass => Input.CtorParamMod.Val(fldName)
                        case _                                      => Input.CtorParamMod.Prv
                      }

                      val isVisible = allMembersAreVisible || termParam.mods.exists(memberVisibiltyAnnot.isDefinedAt(_))
                      val a = adapted(termParam.mods).getOrElse(if (allMembersAreAdapted) Adapted.WithDefaultInteropType else Adapted.No)

                      Input.CtorParam(semSrc, termParam, ctorParamSi.displayName, ctorParamSi, m, isVisible, a)
                    }
                }.toList
              case None => Nil
            }

            val members = recurse(allMembersAreVisible, allMembersAreAdapted, visitChildren)
            builder += Input.Cls(
              semSrc,
              defn,
              effectiveVisibility(defn.mods),
              hasAdaptConstructorAnnot(defn.mods) || hasAdaptAllAnnot(defn.mods),
              si,
              members,
              allMembersAreVisible,
              allMembersAreAdapted,
              ctorParams,
              hasAbstractMod(defn.mods)
            )
          }

        }

        def processObj(defn: Defn.Object, visitChildren: => Unit): Unit = if (!hasPrivateMod(defn.mods)) {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.OBJECT)
          } {
            val allMembersAreVisible = areAllMembersVisible(defn.mods, si)
            val allMembersAreAdapted = areAllMembersAdapted(defn.mods)
            val members              = recurse(allMembersAreVisible, allMembersAreAdapted, visitChildren)
            builder += Input.Obj(semSrc, defn, effectiveVisibility(defn.mods), si, members, allMembersAreVisible, allMembersAreAdapted)
          }

        }

        def processTrait(defn: Defn.Trait, visitChildren: => Unit): Unit = if (!hasPrivateMod(defn.mods)) {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.TRAIT)
          } {
            val members = recurse(areAllMembersVisible(defn.mods, si), areAllMembersAdapted(defn.mods), visitChildren)
            builder += Input.Trait(semSrc, defn, si, members)
          }
        }

        def processType(defn: Defn.Type): Unit = if (!hasPrivateMod(defn.mods)) {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.TYPE)
          } {
            val e = Input.Alias(semSrc, defn, si)
            (e.typeSignature.lowerBound.typeSymbol, e.typeSignature.upperBound.typeSymbol) match {
              case (Some(s1), Some(s2)) if s1 == s2 => builder += e
              case _                                =>
            }
          }
        }

        def process(tree: Tree, visitChildren: => Unit): Unit = tree match {
          case t: Defn.Def    => processDefValVar(t, t.mods, false, Input.Def)
          case t: Defn.Val    => processDefValVar(t, t.mods, false, Input.Val)
          case t: Defn.Var    => processDefValVar(t, t.mods, false, Input.Var)
          case t: Defn.Class  => processCls(t, visitChildren)
          case t: Defn.Object => processObj(t, visitChildren)
          case t: Defn.Trait  => processTrait(t, visitChildren)
          case t: Defn.Type   => processType(t)
          case t: Decl.Def    => processDefValVar(t, t.mods, true, Input.Def)
          case t: Decl.Val    => processDefValVar(t, t.mods, true, Input.Val)
          case t: Decl.Var    => processDefValVar(t, t.mods, true, Input.Var)
          case _              => visitChildren
        }
      }

      val states = mutable.ArrayStack(new State(false, false))

      override def apply(tree: Tree): Unit = {
        states.top.process(tree, super.apply(tree))
      }

    }

    traverser.apply(semSrc.source)

    val inputs = traverser.states.top.builder.result()

    flatten(inputs)
  }

  def topLevel(is: List[Input.Defn]): List[TopLevelExport] = {
    is.collect {
      case i: Input.Exportable if i.isTopLevelExport => TopLevelExport(i.visibility.topLevelExportName.get, i)
    }
  }

  private def flatten(is: List[Input.Defn]): List[Input.Defn] = {
    val b = List.newBuilder[Input.Defn]
    def go(i: Input.Defn): Unit = {
      b += i
      i match {
        case i: Input.ClsOrObj => i.member.foreach(go)
        case i: Input.Trait    => i.member.foreach(go)
        case _                 =>
      }
    }
    is.foreach(go)
    b.result()
  }

  /**
    * @param is flattened list of all input definitions
    */
  def types(is: List[Input.Defn]): List[Input.Type] = is.collect {
    case i: Input.Type => i
  }

}
