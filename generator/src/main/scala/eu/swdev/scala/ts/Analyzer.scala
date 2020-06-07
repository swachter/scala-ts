package eu.swdev.scala.ts

import scala.collection.mutable
import scala.meta.internal.semanticdb.SymbolInformation.Kind
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.meta.internal.semanticdb.{MethodSignature, SymbolInformation, ValueSignature}
import scala.meta.internal.symtab.SymbolTable
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

  def analyze(semSrc: SemSource, symTab: SymbolTable): List[Input.TopLevel] = {

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

    def isSubtypeOfJsAny(si: SymbolInformation): Boolean = si.isSubtypeOf("scala/scalajs/js/Any#", symTab)

    val traverser = new Traverser {

      val builder = List.newBuilder[Input.TopLevel]

      trait State {
        def process(tree: Tree, visitChildren: => Unit): Unit
      }

      object InitialState extends State {

        def processDefValVar[D <: Defn](defn: D,
                                        mods: List[Mod],
                                        ctor: (SemSource, D, SimpleName, SymbolInformation) => Input.TopLevel): Unit = {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.METHOD)
            en <- topLevelExportName(mods)
          } {
            builder += ctor(semSrc, defn, en, si)
          }
        }

        def processCls(defn: Defn.Class, mods: List[Mod], visitChildren: => Unit): Unit = {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.CLASS)
          } {
            val en = topLevelExportName(mods)
            val isJsAny = isSubtypeOfJsAny(si)

            if (en.isDefined || isJsAny) {

              // only classes that have an @ExportTopLevel annotation or that extends js.Any are considered

              val memberBuilder = List.newBuilder[Input.Member]

              val ctorParamTerms = defn.ctor.paramss.flatten

              val isCaseClass = hasCaseClassMod(mods)

              val ctorParams = semSrc.symbolInfo(defn.pos, Kind.CONSTRUCTOR) match {
                case Some(ctorSi) =>
                  val ctorSig = ctorSi.signature.asInstanceOf[MethodSignature]
                  val ctorParamSymbolInfos = ctorSig.parameterLists
                    .flatMap(_.symlinks.map(semSrc.symbolInfo(_)))

                  ctorParamSymbolInfos.flatMap { ctorParamSymbolInfo =>
                    ctorParamTerms.collect {
                      case tp@Term.Param(termMods, termName, termType, defaultTerm)
                        if termName.value == ctorParamSymbolInfo.displayName && !hasPrivateMod(termMods) && hasVarMod(termMods) =>
                        Input.CtorParam(semSrc,
                          tp,
                          SimpleName(ctorParamSymbolInfo.displayName),
                          ctorParamSymbolInfo,
                          Input.CtorParamMod.Var)
                      case tp@Term.Param(termMods, termName, termType, defaultTerm)
                        if termName.value == ctorParamSymbolInfo.displayName && !hasPrivateMod(termMods) && (isCaseClass || hasValMod(
                          termMods)) =>
                        Input.CtorParam(semSrc,
                          tp,
                          SimpleName(ctorParamSymbolInfo.displayName),
                          ctorParamSymbolInfo,
                          Input.CtorParamMod.Val)
                      case tp@Term.Param(termMods, termName, termType, defaultTerm) if termName.value == ctorParamSymbolInfo.displayName =>
                        Input.CtorParam(semSrc,
                          tp,
                          SimpleName(ctorParamSymbolInfo.displayName),
                          ctorParamSymbolInfo,
                          Input.CtorParamMod.Loc)
                    }
                  }.toList
                case None => Nil
              }

              state = InContainerState(memberBuilder, exportAll(mods) || isJsAny)
              visitChildren
              state = InitialState
              builder += Input.Cls(semSrc, defn, en, si, memberBuilder.result(), ctorParams)
            }
          }

        }

        def processObj(defn: Defn.Object, mods: List[Mod], visitChildren: => Unit): Unit = {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.OBJECT)
          } {
            val en = topLevelExportName(mods)
            val isJsAny = isSubtypeOfJsAny(si)

            if (en.isDefined || isJsAny) {
              val memberBuilder = List.newBuilder[Input.Member]
              state = InContainerState(memberBuilder, exportAll(mods) || isJsAny)
              visitChildren
              state = InitialState
              builder += Input.Obj(semSrc, defn, en, si, memberBuilder.result())
            } else {
              visitChildren
            }
          }

        }

        def processTrait(defn: Defn.Trait, visitChildren: => Unit): Unit = {
          for {
            si <- semSrc.symbolInfo(defn.pos, Kind.TRAIT)
          } {
            val memberBuilder = List.newBuilder[Input.Member]
            state = InContainerState(memberBuilder, exportAll(defn.mods) || isSubtypeOfJsAny(si))
            visitChildren
            state = InitialState
            val members = memberBuilder.result()
            // only include traits with exported members
            builder += Input.Trait(semSrc, defn, si, members)
          }
        }

        def processType(defn: Defn.Type): Unit = {
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

        override def process(tree: Tree, visitChildren: => Unit): Unit = tree match {
          case t @ Defn.Def(mods, _, _, _, _, _) => processDefValVar(t, mods, Input.Def)
          case t @ Defn.Val(mods, _, _, _)       => processDefValVar(t, mods, Input.Val)
          case t @ Defn.Var(mods, _, _, _)       => processDefValVar(t, mods, Input.Var)
          case t @ Defn.Class(mods, _, _, _, _)  => processCls(t, mods, visitChildren)
          case t @ Defn.Object(mods, _, _)       => processObj(t, mods, visitChildren)
          case t @ Defn.Trait(mods, _, _, _, _)  => processTrait(t, visitChildren)
          case t @ Defn.Type(_, _, _, _)         => processType(t)
          case _                                 => visitChildren
        }
      }

      case class InContainerState(builder: mutable.Builder[Input.Member, List[Input.Member]], exportAll: Boolean) extends State {

        def processDefValVar[D <: Defn](defn: D,
                                        mods: List[Mod],
                                        ctor: (SemSource, D, SimpleName, SymbolInformation) => Input.Member): Unit = {
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
          case t @ Defn.Def(mods, _, _, _, _, _) => processDefValVar(t, mods, Input.Def)
          case t @ Defn.Val(mods, _, _, _)       => processDefValVar(t, mods, Input.Val)
          case t @ Defn.Var(mods, _, _, _)       => processDefValVar(t, mods, Input.Var)
          case t @ Defn.Class(_, _, _, _, _)     => ()
          case t @ Defn.Object(_, _, _)          => ()
          case t @ Defn.Trait(_, _, _, _, _)     => ()
          case _                                 => visitChildren
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

  // determine all types that are referenced in the given export item
  // -> type parameters are not considered
  def referencedTypes(e: Input, symTab: SymbolTable): List[isb.Type] = referencedTypes(e).filter(!_.isTypeParam(symTab))

  private def referencedTypes(e: Input): List[isb.Type] = e match {
    case e: Input.Def       => e.methodSignature.returnType :: parameterTypes(e)
    case e: Input.Val       => List(e.methodSignature.returnType)
    case e: Input.Var       => List(e.methodSignature.returnType)
    case e: Input.Cls       => e.member.flatMap(referencedTypes) ++ e.ctorParams.flatMap(referencedTypes)
    case e: Input.Obj       => e.member.flatMap(referencedTypes)
    case e: Input.Trait       => e.member.flatMap(referencedTypes)
    case e: Input.Alias       => Nil
    case e: Input.CtorParam => List(e.valueSignature.tpe)
  }

  private def parameterTypes(e: Input.Def): List[isb.Type] = {
    def argType(symbol: String): isb.Type = {
      val si = e.semSrc.symbolInfo(symbol)
      val vs = si.signature.asInstanceOf[ValueSignature]
      vs.tpe
    }
    e.methodSignature.parameterLists.flatMap(_.symlinks).map(argType).toList
  }

}
