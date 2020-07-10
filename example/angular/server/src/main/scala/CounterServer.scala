package org.test.server

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{HttpApp, Route}
import endpoints4s.akkahttp.server
import org.test.shared.{Counter, CounterEndpoints}
import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}

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

/**
 * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
 */
object CounterDocumentation
  extends CounterEndpoints
    with openapi.Endpoints
    with openapi.JsonEntitiesFromSchemas {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(currentValue, increment)

}

object DocumentationServer
  extends server.Endpoints
    with server.JsonEntitiesFromEncodersAndDecoders {

  val routes =
    endpoint(get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => CounterDocumentation.api)

}

object WebServer extends HttpApp {
  override protected def routes: Route = CounterServer.routes ~ DocumentationServer.routes

  def main(args: Array[String]): Unit = startServer("localhost", 8000)
}