package x

import eu.swdev.scala.ts.annotation._

object Util {

  @Adapt("js.UndefOr[js.Array[X]]")
  def sequence[X](ol: List[Option[X]]): Option[List[X]] = ol.foldRight(Option(List.empty[X]))((o, a) => a.flatMap(l => o.map(_ :: l)))

}
