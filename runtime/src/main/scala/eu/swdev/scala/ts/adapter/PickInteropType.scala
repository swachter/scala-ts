package eu.swdev.scala.ts.adapter

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Determines which interoperability type should be used for a given type.
  *
  * For each return type of a method, type of a val, var, or parameter, a corresponding interop type must be picked
  * that is suitable for JavaScript.
  *
  * @tparam F the given type
  * @tparam T the picked interoperability type
  */
trait PickInteropType[F, T]

trait LowPrioPickInteropType {
  implicit def id[X]: PickInteropType[X, X] = null
}

object PickInteropType extends LowPrioPickInteropType {
  implicit def array[F, T](implicit ev: PickInteropType[F, T]): PickInteropType[Array[F], js.Array[T]]     = null
  implicit def future[F, T](implicit ev: PickInteropType[F, T]): PickInteropType[Future[F], js.Promise[T]] = null
  implicit def list[F, T](implicit ev: PickInteropType[F, T]): PickInteropType[List[F], js.Array[T]]       = null
  implicit def option[F, T](implicit ev: PickInteropType[F, T]): PickInteropType[Option[F], js.UndefOr[T]] = null
}
