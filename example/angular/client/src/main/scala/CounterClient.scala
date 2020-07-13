package org.test.client

import endpoints4s.xhr
import org.test.shared.{Counter, CounterEndpoints, Increment}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportTopLevel

/**
 * Defines an HTTP client for the endpoints described in the `CounterEndpoints` trait.
 * The derived HTTP client uses XMLHttpRequest to perform requests and returns
 * results in a `js.Thenable`.
 */
@JSExportTopLevel("CounterClient")
object CounterClient extends js.Object {

  private object client
    extends CounterEndpoints
    with xhr.thenable.Endpoints
    with xhr.JsonEntitiesFromSchemas

  /**
   * Performs an XMLHttpRequest on the `currentValue` endpoint, and then
   * deserializes the JSON response as a `Counter`.
   */
  def currentValue(): js.Promise[Counter] /*: js.Thenable[Counter] */ = client.currentValue(()).toFuture.toJSPromise

  /**
   * Serializes the `Increment` value into JSON and performs an XMLHttpRequest
   * on the `increment` endpoint.
   */
  def increment(value: Int): js.Promise[Unit] /*: js.Thenable[Unit] */ = client.increment(Increment(value)).toFuture.toJSPromise
}
