package scala.scalajs

import scala.annotation.unchecked.uncheckedVariance

package object js {

  type UndefOr[+A] = (A @uncheckedVariance) | Unit

  sealed trait |[A, B]

}
