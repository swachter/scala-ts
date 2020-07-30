//package x
//
//import eu.swdev.scala.ts.annotation._
//
//object Util {
//
//  @Adapt
//  def traverse[X](ol: List[Option[X]]): Option[List[X]] = ol.foldRight(Option(List.empty[X]))((o, a) => a.flatMap(l => o.map(_ :: l)))
//
//}
