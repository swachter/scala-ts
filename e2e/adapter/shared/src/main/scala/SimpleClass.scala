package x.y

import eu.swdev.scala.ts.annotation.{Adapt, AdaptAll}

@AdaptAll
class SimpleClass(var x: Array[Int]) {

  def sum = x.sum

  def filter(p: Int => Boolean) = x =  x.filter(p)

}

object SimpleClass {

  @Adapt
  def fromInt(int: Int) = new SimpleClass(Array(int))
}
