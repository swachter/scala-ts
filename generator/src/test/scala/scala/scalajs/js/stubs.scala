package scala.scalajs.js

import scala.scalajs.js.annotation.JSGlobal

trait Any extends scala.AnyRef

class Object extends Any

class Array[A]

class Function extends Object

trait Function0[+R] extends Function

trait Function1[-T1, +R] extends Function

trait Function2[-T1, -T2, +R] extends Function

trait ThisFunction extends Function

trait ThisFunction0[-T0, +R] extends ThisFunction

trait ThisFunction1[-T0, -T1, +R] extends ThisFunction

trait ThisFunction2[-T0, -T1, -T2, +R] extends ThisFunction

sealed trait Tuple2[+T1, +T2] extends Object

sealed trait Tuple3[+T1, +T2, +T3] extends Object

sealed trait |[A, B]

sealed trait Dictionary[A] extends Any

class Promise[+A] extends Object

class Date extends Object

class RegExp(pattern: String, flags: String = "") extends Object

sealed trait Symbol extends Any

@JSGlobal("Symbol")
object Symbol extends Object {
  /** Creates a new unique symbol without description.
   *
   *  @group factories
   */
  def apply(): Symbol = null

  /** Creates a new unique symbol with the specified description.
   *
   *  @group factories
   */
  def apply(description: String): Symbol = null

  /** Retrieves the symbol with the specified key in the global symbol registry.
   *
   *  The returned symbol's description is also the key.
   *
   *  Asking twice `forKey` with the same key returns the same symbol,
   *  globally.
   *
   *  @group registry
   */
  def forKey(key: String): Symbol = null

  /** Retrieves the key under which the specified symbol is registered in the
   *  global symbol registry, or `undefined` if it is not registered.
   *
   *  @group registry
   */
  def keyFor(sym: Symbol): UndefOr[String] = null

  /** The well-known symbol `@@hasInstance`.
   *
   *  @group wellknownsyms
   */
  val hasInstance: Symbol = null

  /** The well-known symbol `@@isConcatSpreadable`.
   *
   *  @group wellknownsyms
   */
  val isConcatSpreadable: Symbol = null

  /** The well-known symbol `@@iterator`.
   *
   *  @group wellknownsyms
   */
  val iterator: Symbol = null

  /** The well-known symbol `@@match`.
   *
   *  @group wellknownsyms
   */
  val `match`: Symbol = null

  /** The well-known symbol `@@replace`.
   *
   *  @group wellknownsyms
   */
  val replace: Symbol = null

  /** The well-known symbol `@@search`.
   *
   *  @group wellknownsyms
   */
  val search: Symbol = null

  /** The well-known symbol `@@species`.
   *
   *  @group wellknownsyms
   */
  val species: Symbol = null

  /** The well-known symbol `@@split`.
   *
   *  @group wellknownsyms
   */
  val split: Symbol = null

  /** The well-known symbol `@@toPrimitive`.
   *
   *  @group wellknownsyms
   */
  val toPrimitive: Symbol = null

  /** The well-known symbol `@@toStringTag`.
   *
   *  @group wellknownsyms
   */
  val toStringTag: Symbol = null

  /** The well-known symbol `@@unscopables`.
   *
   *  @group wellknownsyms
   */
  val unscopables: Symbol = null
}


trait Iterator[+A] extends Object

trait Iterable[+A] extends Object