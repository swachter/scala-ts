package eu.swdev.scala.ts

import scala.collection.mutable

class AdapterObject(val name: String) {

  val nested  = mutable.SortedMap.empty[String, AdapterObject]
  val methods = mutable.SortedMap.empty[String, Adaption.Method]
  val traits  = mutable.SortedMap.empty[String, Adaption.Trait]

  def +=(a: Adaption.Method): this.type = {
    enclosingAdapterObject(a.fullName).methods += a.simpleName -> a
    this
  }

  def +=(a: Adaption.Trait): this.type = {
    enclosingAdapterObject(a.fullName).traits += a.simpleName -> a
    this
  }

  private def enclosingAdapterObject(fullName: FullName): AdapterObject = {
    var n = this
    fullName.str.split('.').dropRight(1).foreach { p =>
      n = n.nested.getOrElseUpdate(p, new AdapterObject(p))
    }
    n
  }

}

object AdapterObject {

  def apply(inputs: List[Input.Defn]): AdapterObject = {

    val root = new AdapterObject("")

    inputs
      .collect {
        case i: Input.Obj => i
      }
      .foreach { obj =>
        obj.member
          .collect {
            case i: Input.DefOrValOrVar if obj.allMembersAreAdapted || i.adapted.isAdapted => i
          }
          .foreach {
            case i: Input.Def => root += Adaption.Def(i)
            case i: Input.Val => root += Adaption.Val(i)
            case i: Input.Var => root += Adaption.Var(i)
          }
      }

    inputs.collect {
      case i: Input.Cls => i
    }.foreach { cls =>
      if (cls.constrAdapted) {
        root += Adaption.NewInstance(cls)
      }
      if (cls.member.exists(isAdapted)) {
        root += Adaption.NewAdapter(cls)
        root += Adaption.Trait(cls)
      }
    }

    root
  }

  def isAdapted(input: Input.Defn): Boolean = input match {
    case i: Input.DefOrValOrVar => i.adapted.isAdapted
    case _: Input.Type => false
  }

}
