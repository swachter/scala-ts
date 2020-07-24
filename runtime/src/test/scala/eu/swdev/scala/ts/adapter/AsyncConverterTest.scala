package eu.swdev.scala.ts.adapter

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.must.Matchers

import scala.concurrent.Future
import scala.scalajs.js

class AsyncConverterTest extends AsyncFunSuite with Matchers {

  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.queue

  test("Promise<->Future") {
    def method[X](a: Future[X]) = a
    val r: js.Promise[Int]      = $res(method(js.Promise.resolve[Int](1).$cnv/*.[Future[Int]]*/))
    r.toFuture.map(_ mustBe 1)
  }

}
