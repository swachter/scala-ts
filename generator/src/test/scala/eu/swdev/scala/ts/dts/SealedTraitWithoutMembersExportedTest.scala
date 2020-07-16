package dts

import eu.swdev.scala.ts.DtsFunSuite

class SealedTraitWithoutMembersExportedTest extends DtsFunSuite {

  """
    |""".check()

}

object SealedTraitWithoutMembersExportedTest {

  sealed trait Base
  sealed trait Sub

}