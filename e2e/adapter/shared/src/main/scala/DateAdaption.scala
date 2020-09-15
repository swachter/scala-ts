package t

import java.util.Date

import eu.swdev.scala.ts.annotation.Adapt

object DateAdaption {

  @Adapt
  var date: Date = new Date()

  @Adapt("Double")
  var doubleDate: Date = new Date()

}
