package eu.swdev.scala.ts

sealed trait Adaption {
  def fullName: FullName
  def simpleName: String = fullName.last
  def input: Input
}

object Adaption {

  sealed trait Method extends Adaption
  sealed trait DefValVar extends Method {
    def fullName: FullName = FullName(input.si)
  }

  case class Def(input: Input.Def) extends DefValVar
  case class Val(input: Input.Val) extends DefValVar
  case class Var(input: Input.Var) extends DefValVar

  // the newDelegate and newAdapter methods must be part of the adapter object of the class
  // -> construct the full name by adding "newDelegate" or "newAdapter" to the full name of the class

  case class NewDelegate(input: Input.Cls) extends Method {
    def fullName = FullName(input.si).member("newDelegate")
  }
  case class NewAdapter(input: Input.Cls) extends Method {
    def fullName = FullName(input.si).member("newAdapter")
  }

  case class Trait(input: Input.Cls) extends Adaption {
    def fullName: FullName = FullName(input.si)
  }

}
