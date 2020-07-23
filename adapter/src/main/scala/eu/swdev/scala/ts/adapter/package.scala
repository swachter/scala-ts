package eu.swdev.scala.ts

package object adapter {

  implicit class From[X](val x: X) extends AnyVal {
      def convert[Y](implicit ev: Converter[X, Y]): Y = ev(x)
  }

  def result[X, Y](x: X)(implicit ev1: JavaScriptType[X, Y], ev2: Converter[X, Y]): Y = ev2(x)
}
