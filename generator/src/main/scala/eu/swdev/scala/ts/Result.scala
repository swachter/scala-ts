package eu.swdev.scala.ts

trait Result {

  def openBlock(line: String): Unit

  def closeBlock(): Unit

  def addLine(line: String): Unit
}

object Result {

  class StringBuilderResult extends Result {
    var indent = 0
    def space = "  " * indent

    val sb = new StringBuilder

    override def openBlock(line: String): Unit = {
      sb.append(s"$space$line {\n")
      indent += 1
    }

    override def closeBlock(): Unit = {
      indent -= 1
      sb.append(s"$space}\n")
    }

    override def addLine(line: String): Unit = sb.append(s"$space$line\n")
  }
}
