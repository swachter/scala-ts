package dts

import eu.swdev.scala.ts.DtsFunSuite

class SealedTraitNoMembersExportedTest extends DtsFunSuite {

  """
    |""".check()

}

object SealedTraitNoMembersExportedTest {
  sealed trait Base
  sealed trait Sub extends Base

  class Case1 extends Base

  class Case2 extends Sub

  class Case3 extends Sub

}












