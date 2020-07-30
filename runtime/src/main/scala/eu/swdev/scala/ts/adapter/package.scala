package eu.swdev.scala.ts

package object adapter {

  /**
    * Captures the the type of the value that is converted.
    */
  implicit class InteropFrom[F](val f: F) extends AnyVal {
    def $cnv[T](implicit ev: InteropConverter[F, T]): T = ev(f)

    def $res[T](implicit ev0: PickInteropType[F, T], ev: InteropConverter[F, T]): T = ev(f)
  }
}
