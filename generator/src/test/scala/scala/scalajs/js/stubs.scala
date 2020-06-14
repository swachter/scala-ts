package scala.scalajs.js

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

trait Iterator[+A] extends Object

trait Iterable[+A] extends Object