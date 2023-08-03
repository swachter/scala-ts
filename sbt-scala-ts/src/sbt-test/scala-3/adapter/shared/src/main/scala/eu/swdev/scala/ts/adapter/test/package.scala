package eu.swdev.scala.ts.adapter

import eu.swdev.scala.ts.annotation.Adapt

package object test {

  @Adapt
  def min(list: List[Double]): Double = list.min

}
