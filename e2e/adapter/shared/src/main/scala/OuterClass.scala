package o

import eu.swdev.scala.ts.annotation.AdaptAll

//@AdaptAll
class OuterClass(val x: Int) {
//  @AdaptAll
  class Inner(val y: String)
}
