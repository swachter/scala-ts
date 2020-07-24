package eu.swdev.scala.ts

package object adapter {

  /**
    * Captures the the type of the value that is converted.
    */
  implicit class InteropFrom[F](val f: F) extends AnyVal {
    def $cnv[T](implicit ev: InteropConverter[F, T]): T = ev(f)
  }

  def $res[X, Y](x: X)(implicit ev1: PickInteropType[X, Y], ev2: InteropConverter[X, Y]): Y = ev2(x)
}
