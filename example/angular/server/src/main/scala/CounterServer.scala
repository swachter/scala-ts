package org.test.server

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{HttpApp, Route}
import endpoints4s.akkahttp.server
import org.test.shared.{Counter, CounterEndpoints}

/**
 * Defines Akka HTTP routes for the endpoints described in the `CounterEndpoints` trait.
 */
object CounterServer
  extends CounterEndpoints
    with server.Endpoints
    with server.JsonEntitiesFromSchemas {

  /** Simple implementation of an in-memory counter */
  val counter = new AtomicInteger()

  // Implements the `currentValue` endpoint
  val currentValueRoute = currentValue.implementedBy(_ => Counter(counter.get))

  // Implements the `increment` endpoint
  val incrementRoute = increment.implementedBy(inc => counter.addAndGet(inc.step))

  val routes: Route = currentValueRoute ~ incrementRoute

}

object WebServer extends HttpApp {
  override protected def routes: Route = CounterServer.routes

  def main(args: Array[String]): Unit = startServer("localhost", 8000)
}