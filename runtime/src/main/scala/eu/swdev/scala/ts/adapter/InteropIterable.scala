package eu.swdev.scala.ts.adapter

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSName

class InteropIterable[A, B](iter: Iterable[A], f: A => B) extends js.Iterable[B] {
  @JSName(js.Symbol.iterator)
  override def jsIterator(): js.Iterator[B] = iter.map(f).iterator.toJSIterator
}
