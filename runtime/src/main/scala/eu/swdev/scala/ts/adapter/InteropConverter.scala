package eu.swdev.scala.ts.adapter

import java.util.Date

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.concurrent.JSExecutionContext.Implicits._

trait InteropConverter[F, T] {
  def apply(f: F): T
}

/**
 * Transforms to and from interoperability types.
 */
trait LowPrio {

  protected type C[F, T] = InteropConverter[F, T]

  implicit def idConverter[X]: C[X, X] = identity

}

object InteropConverter extends LowPrio {

  implicit def jsArray2ScalaArray[X: ClassTag, Y: ClassTag](implicit ev: C[X, Y]): C[js.Array[X], Array[Y]] = _.toArray.map(ev(_))
  implicit def jsArray2ScalaList[X, Y](implicit ev: C[X, Y]): C[js.Array[X], List[Y]] = _.toList.map(ev(_))

  implicit def scalaArray2JsArray[X, Y](implicit ev: C[X, Y]): C[Array[X], js.Array[Y]] = _.toJSArray.map(ev(_))
  implicit def scalaList2JsArray[X, Y](implicit ev: C[X, Y]): C[List[X], js.Array[Y]] = _.toJSArray.map(ev(_))

  implicit def undefOr2Option[X, Y](implicit ev: C[X, Y]): C[js.UndefOr[X], Option[Y]] = _.toOption.map(ev(_))
  implicit def option2undefOr[X, Y](implicit ev: C[X, Y]): C[Option[X], js.UndefOr[Y]] = _.orUndefined.map(ev(_))

  implicit def promise2Future[X, Y](implicit ev: C[X, Y]): C[js.Promise[X], Future[Y]] = _.toFuture.map(ev(_))
  implicit def future2Promise[X, Y](implicit ev: C[X, Y]): C[Future[X], js.Promise[Y]] = _.map(ev(_)).toJSPromise

  implicit val jsDate2Date: C[js.Date, Date] = d => new Date(d.getTime().toLong)
  implicit val date2JsDate: C[Date, js.Date] = d => new js.Date(d.getTime())

  implicit def jsf0[F, T](implicit r: C[F, T]): C[js.Function0[F], () => T] = f => () => r(f())
  implicit def jsf1[F0, T0, F, T](implicit r: C[F, T], c0: C[T0, F0]): C[js.Function1[F0, F], (T0) => T] = f => p0 => r(f(c0(p0)))
  implicit def jsf2[F0, T0, F1, T1, F, T](implicit r: C[F, T], c0: C[T0, F0], c1: C[T1, F1]): C[js.Function2[F0, F1, F], (T0, T1) => T] = f => (p0, p1) => r(f(c0(p0), c1(p1)))
  implicit def jsf3[F0, T0, F1, T1, F2, T2, F, T](implicit r: C[F, T], c0: C[T0, F0], c1: C[T1, F1], c2: C[T2, F2]): C[js.Function3[F0, F1, F2, F], (T0, T1, T2) => T] = f => (p0, p1, p2) => r(f(c0(p0), c1(p1), c2(p2)))
  implicit def jsf4[F0, T0, F1, T1, F2, T2, F3, T3, F, T](implicit r: C[F, T], c0: C[T0, F0], c1: C[T1, F1], c2: C[T2, F2], c3: C[T3, F3]): C[js.Function4[F0, F1, F2, F3, F], (T0, T1, T2, T3) => T] = f => (p0, p1, p2, p3) => r(f(c0(p0), c1(p1), c2(p2), c3(p3)))

  implicit def f0[F, T](implicit r: C[F, T]): C[() => F, js.Function0[T]] = f => () => r(f())
  implicit def f1[F0, T0, F, T](implicit r: C[F, T], c0: C[T0, F0]): C[(F0) => F, js.Function1[T0, T]] = f => p0 => r(f(c0(p0)))
  implicit def f2[F0, T0, F1, T1, F, T](implicit r: C[F, T], c0: C[T0, F0], c1: C[T1, F1]): C[(F0, F1) => F, js.Function2[T0, T1, T]] = f => (p0, p1) => r(f(c0(p0), c1(p1)))
  implicit def f3[F0, T0, F1, T1, F2, T2, F, T](implicit r: C[F, T], c0: C[T0, F0], c1: C[T1, F1], c2: C[T2, F2]): C[(F0, F1, F2) => F, js.Function3[T0, T1, T2, T]] = f => (p0, p1, p2) => r(f(c0(p0), c1(p1), c2(p2)))
  implicit def f4[F0, T0, F1, T1, F2, T2, F3, T3, F, T](implicit r: C[F, T], c0: C[T0, F0], c1: C[T1, F1], c2: C[T2, F2], c3: C[T3, F3]): C[(F0, F1, F2, F3) => F, js.Function4[T0, T1, T2, T3, T]] = f => (p0, p1, p2, p3) => r(f(c0(p0), c1(p1), c2(p2), c3(p3)))

  implicit def jst2[F1, T1, F2, T2](implicit c1: C[F1, T1], c2: C[F2, T2]): C[js.Tuple2[F1, F2], (T1, T2)] = t => (c1(t._1), c2(t._2))
  implicit def jst3[F1, T1, F2, T2, F3, T3](implicit c1: C[F1, T1], c2: C[F2, T2], c3: C[F3, T3]): C[js.Tuple3[F1, F2, F3], (T1, T2, T3)] = t => (c1(t._1), c2(t._2), c3(t._3))
  implicit def jst4[F1, T1, F2, T2, F3, T3, F4, T4](implicit c1: C[F1, T1], c2: C[F2, T2], c3: C[F3, T3], c4: C[F4, T4]): C[js.Tuple4[F1, F2, F3, F4], (T1, T2, T3, T4)] = t => (c1(t._1), c2(t._2), c3(t._3), c4(t._4))
  implicit def jst5[F1, T1, F2, T2, F3, T3, F4, T4, F5, T5](implicit c1: C[F1, T1], c2: C[F2, T2], c3: C[F3, T3], c4: C[F4, T4], c5: C[F5, T5]): C[js.Tuple5[F1, F2, F3, F4, F5], (T1, T2, T3, T4, T5)] = t => (c1(t._1), c2(t._2), c3(t._3), c4(t._4), c5(t._5))

  implicit def t2[F1, T1, F2, T2](implicit c1: C[F1, T1], c2: C[F2, T2]): C[(F1, F2), js.Tuple2[T1, T2]] = t => (c1(t._1), c2(t._2))
  implicit def t3[F1, T1, F2, T2, F3, T3](implicit c1: C[F1, T1], c2: C[F2, T2], c3: C[F3, T3]): C[(F1, F2, F3), js.Tuple3[T1, T2, T3]] = t => (c1(t._1), c2(t._2), c3(t._3))
  implicit def t4[F1, T1, F2, T2, F3, T3, F4, T4](implicit c1: C[F1, T1], c2: C[F2, T2], c3: C[F3, T3], c4: C[F4, T4]): C[(F1, F2, F3, F4), js.Tuple4[T1, T2, T3, T4]] = t => (c1(t._1), c2(t._2), c3(t._3), c4(t._4))
  implicit def t5[F1, T1, F2, T2, F3, T3, F4, T4, F5, T5](implicit c1: C[F1, T1], c2: C[F2, T2], c3: C[F3, T3], c4: C[F4, T4], c5: C[F5, T5]): C[(F1, F2, F3, F4, F5), js.Tuple5[T1, T2, T3, T4, T5]] = t => (c1(t._1), c2(t._2), c3(t._3), c4(t._4), c5(t._5))
}

