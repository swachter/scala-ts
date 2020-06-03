package eu.swdev.scala.ts

import scala.collection.mutable


class Namespace(val name: SimpleName) {

  val nested = mutable.SortedMap.empty[SimpleName, Namespace]
  val itfs   = mutable.SortedMap.empty[SimpleName, Interface]
  val unions = mutable.SortedMap.empty[SimpleName, Union]

  def +=(itf: Interface): this.type = {
    enclosingNamespace(itf.fullName).itfs += itf.simpleName -> itf
    this
  }

  def +=(union: Union): this.type = {
    enclosingNamespace(union.fullName).unions += union.fullName.last -> union
    this
  }

  def containsItf(fullName: FullName): Boolean = {
    val sn = fullName.head
    fullName.tail match {
      case Some(fn) => nested.get(sn).map(_.containsItf(fn)).getOrElse(false)
      case None => itfs.contains(sn)
    }
  }

  private def enclosingNamespace(fullName: FullName): Namespace = {
    var n = this
    fullName.str.split('.').dropRight(1).map(SimpleName(_)).foreach { p =>
      n = n.nested.getOrElseUpdate(p, new Namespace(p))
    }
    n
  }

}
