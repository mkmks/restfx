package org.mkmks.restfx

import cats.effect.{IO, Resource}
import io.circe.parser._
import munit.CatsEffectSuite
import fs2.Stream
import org.http4s.{Response}
import org.http4s.client.Client

class ConvertSpec extends CatsEffectSuite {

  /* While this test might appear trivial, JSON schema definition with Circe is
   * still not a pushbutton affair, and it might not have worked from the first
   * attempt. In fact, it didn't. */

  test("Order decoder works") {
    val orderJson = """{ "fromCurrency": "GBP", "toCurrency" : "EUR", "amount" : 102.6 }"""
    val orderAdt = Right(Order(GBP,EUR,102.6))
    assert(decode[Order](orderJson) == orderAdt, ())
  }

  def client(body: String): Client[IO] = Client.apply[IO] { _ =>
    Resource.eval(
      IO(Response[IO](body = Stream.emits(body.getBytes("UTF-8")))))
  }

  val order = Order(GBP, EUR, 102.6)

  test("Give an order, receive an offer") {
    val convertAlg = Convert.impl[IO](client("""{ "EUR": 1.164659 }"""))
    val offer = Offer(1.164659, 102.6, 119.4940134)
    assertIO(convertAlg.convert(order), Some(offer))
  }

  test("Fail if the received quotes don't list the currency ordered") {
    val convertAlg = Convert.impl[IO](client("""{ "USD": 1.362250 }"""))
    assertIO(convertAlg.convert(order), None)
  }

}
