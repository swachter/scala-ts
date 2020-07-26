package e2e

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/* .d.ts:

export interface ClassWithInheritedMethods1 extends e2e.TraitInheritance1.Middle {
  'ClassWithInheritedMethods1': never
}
export class ClassWithInheritedMethods1 {
  constructor()
}
export namespace e2e {
  namespace TraitInheritance1 {
    interface Base {
      base(n: number): number
      'e2e.TraitInheritance1.Base': never
    }
    interface Middle extends e2e.TraitInheritance1.Base {
      middle(n: number): number
      'e2e.TraitInheritance1.Middle': never
    }
  }
}

 */

object TraitInheritance1 {
  @JSExportAll
  trait Base {
    def base(n: Int) = 2*n
  }

  @JSExportAll
  trait Middle extends Base {
    def middle(n: Int) = 3*n
  }

  @JSExportTopLevel("ClassWithInheritedMethods1")
  class Cls extends Middle

}
