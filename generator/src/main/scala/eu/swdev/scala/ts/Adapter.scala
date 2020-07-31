package eu.swdev.scala.ts

import scala.collection.mutable

sealed trait Adapter {
  def name: String
}

object Adapter {

  def apply(inputs: Inputs, adapterName: String): Adapter.Obj = {
    val root = Obj(adapterName)

    def traverse(defn: Input.Defn): Unit = {
      def owner(i: Input) = root.enclosingObj(FullName(i.si))
      defn match {
        case i: Input.DefOrValOrVar if i.adapted.isAdapted => owner(i) += DefValVar(i)
        case i: Input.Obj                                  => i.member.foreach(traverse(_))
        case i: Input.Cls                                  => addClassAdaption(i, owner(i), ())
        case _                                             =>
      }
    }
    inputs.list.foreach(traverse(_))

    root
  }

  def addMemberToTrait(owner: Trait, i: Input.Defn): Unit = {
    i match {
      case i: Input.DefOrValOrVar if i.adapted.isAdapted => owner += DefValVar(i)
      case i: Input.Obj                                  => addObjAdaption(i, owner)
      case i: Input.Cls                                  => addClassAdaption(i, owner, owner += AdapterObjProp(i))
    }
  }

  def addMemberToObj(owner: Obj, i: Input.Defn): Unit = {
    i match {
      case i: Input.DefOrValOrVar if i.adapted.isAdapted => owner += DefValVar(i)
      case i: Input.Obj                                  => addObjAdaption(i, owner)
      case i: Input.Cls                                  => addClassAdaption(i, owner, ())
    }
  }

  def addObjAdaption[D <: Def](i: Input.Obj, owner: Container[D]): Unit = {
    val obj = Obj(i.si.displayName)
    owner += obj
    i.member.foreach(addMemberToObj(obj, _))
  }

  def addClassAdaption[D <: Def](i: Input.Cls, owner: Container[D], addAdapterObjProperty: => Unit): Unit = {
    def adapterObj = owner.nestedObj(i.si.displayName)
    if (i.constrAdapted) {
      adapterObj += NewDelegateDef(i)
      addAdapterObjProperty
    }
    if (i.member.exists(isAdapted) || i.ctorParams.exists(_.adapted.isAdapted)) {
      adapterObj += NewAdapterDef(i)
      val t = Trait(i)
      owner += t
      i.member.foreach(addMemberToTrait(t, _))
      i.ctorParams.filter(_.adapted.isAdapted).foreach(t += CtorParam(_))
    }
  }

  def isAdapted(input: Input.Defn): Boolean = input match {
    case i: Input.DefOrValOrVar => i.adapted.isAdapted
    case _: Input.Type          => false
  }

  sealed trait WithInput {
    def input: Input
  }

  sealed trait WithNameFromDisplayName extends WithInput {
    def name = input.si.displayName
  }

  sealed trait Def extends Adapter

  sealed trait DefInObj extends Def

  sealed trait DefInTrait extends Def

  sealed trait Container[DEF <: Def] extends Adapter {

    val defs   = mutable.SortedMap.empty[String, DEF]
    val objs   = mutable.SortedMap.empty[String, Obj]
    val traits = mutable.SortedMap.empty[String, Trait]

    def +=(a: DEF): Unit   = defs += a.name   -> a
    def +=(a: Obj): Unit   = objs += a.name   -> a
    def +=(a: Trait): Unit = traits += a.name -> a

    def nestedObj(name: String): Obj = {
      objs.getOrElseUpdate(name, Obj(name))
    }
  }

  case class DefValVar(input: Input.DefOrValOrVar) extends DefInTrait with DefInObj with WithNameFromDisplayName

  case class CtorParam(input: Input.CtorParam) extends DefInTrait with WithNameFromDisplayName

  case class NewDelegateDef(input: Input.Cls) extends DefInObj {
    def name = "newDelegate"
  }

  case class NewAdapterDef(input: Input.Cls) extends DefInObj {
    def name = "newAdapter"
  }

  case class AdapterObjProp(input: Input.Cls) extends DefInTrait {
    override def name: String = s"${input.si.displayName}$$a"
  }

  case class Trait(input: Input.Cls) extends Container[DefInTrait] with WithNameFromDisplayName

  case class Obj(name: String) extends Container[DefInObj] {
    def enclosingObj(fullName: FullName): Obj = {
      var o = this
      fullName.str.split('.').dropRight(1).foreach { p =>
        o = o.nestedObj(p)
      }
      o
    }
    def hasTraits: Boolean = traits.nonEmpty || objs.values.exists(_.hasTraits)
  }

}
