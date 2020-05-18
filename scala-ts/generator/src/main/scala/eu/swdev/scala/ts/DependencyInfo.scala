package eu.swdev.scala.ts

import java.io.File

class DependencyInfo(files: Seq[File]) extends SemanticDbInfo(files) {

}

object DependencyInfo {

  def apply(metacp: Set[File]): DependencyInfo = {
    new DependencyInfo(metacp.toSeq)
  }

}
