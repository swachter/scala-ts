package e2e

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

/* .d.ts:

export interface ClassWithInheritedMethods2 extends e2e.TraitInheritance2.Middle {
  'ClassWithInheritedMethods2': never
}
export class ClassWithInheritedMethods2 {
  constructor()
  middle(n: number): number
  base(n: number): number
}
export namespace e2e {
  namespace TraitInheritance2 {
    interface Base {
      base(n: number): number
      'e2e.TraitInheritance2.Base': never
    }
    interface Middle extends e2e.TraitInheritance2.Base {
      middle(n: number): number
      'e2e.TraitInheritance2.Middle': never
    }
  }
}

 */

object TraitInheritance2 {

  trait Base extends js.Object {
    def base(n: Int): Int
  }

  trait Middle extends Base {
    def middle(n: Int): Int
  }

  @JSExportTopLevel("ClassWithInheritedMethods2")
  class Cls extends Middle {
    override def middle(n: Int): Int = 3*n
    override def base(n: Int): Int = 2*n
  }

}
