package x.y

import eu.swdev.scala.ts.annotation.AdaptAll

@AdaptAll
class SimpleClass(var x: Array[Int]) {

  def sum = x.sum

  def filter(p: Int => Boolean) = x =  x.filter(p)

}
