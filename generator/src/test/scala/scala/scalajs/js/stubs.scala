package scala.scalajs.js

trait Any extends scala.AnyRef

class Object extends Any

class Array[A]

class Function extends Object

trait Function0[+R] extends Function

trait Function1[-T1, +R] extends Function

trait Function2[-T1, -T2, +R] extends Function

sealed trait Tuple2[+T1, +T2] extends Object

sealed trait Tuple3[+T1, +T2, +T3] extends Object

sealed trait |[A, B]

sealed trait Dictionary[A] extends Any

class Promise[+A] extends Object