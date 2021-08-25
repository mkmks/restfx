package org.mkmks.restfx

import cats.effect.Concurrent
import cats.syntax.functor._
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.deriveEnumerationCodec
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._

// Protocol messages

sealed trait Currency
case object GBP extends Currency
case object USD extends Currency
case object EUR extends Currency
case object CHF extends Currency
case object CNY extends Currency

object Currency {

  implicit val currencyCodec: Codec[Currency] = deriveEnumerationCodec[Currency]
  
  implicit val currencyKeyEncoder: KeyEncoder[Currency] = new KeyEncoder[Currency] {
    override def apply(key: Currency) = key.toString()
  }

  implicit val currencyKeyDecoder: KeyDecoder[Currency] = new KeyDecoder[Currency] {
    override def apply(key: String): Option[Currency] = key match {
      case "GBP" => Some(GBP)
      case "USD" => Some(USD)
      case "EUR" => Some(EUR)
      case "CHF" => Some(CHF)
      case "CNY" => Some(CNY)
      case _: String  => None
    }
  }

  type Quotes = Map[Currency, BigDecimal]

  implicit def quotesEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, Quotes] = jsonOf

}

case class Order(fromCurrency: Currency, toCurrency: Currency, amount: BigDecimal)

object Order {
  implicit val orderDecoder: Decoder[Order] = deriveDecoder[Order]
  implicit def orderEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, Order] = jsonOf
}

case class Offer(exchange: BigDecimal, amount: BigDecimal, original: BigDecimal)

object Offer {
  implicit val offerEncoder: Encoder[Offer] = deriveEncoder[Offer]
  implicit def offerEntityEncoder[F[_]]: EntityEncoder[F, Offer] = jsonEncoderOf
}

// Tagless final algebra for protocol implementations

trait Convert[F[_]] {
  def convert(order: Order): F[Option[Offer]]
}

// Protocol implementation

object Convert {
  def impl[F[_]: Concurrent](C: Client[F]) = new Convert[F] {
    val dsl = new Http4sClientDsl[F]{}
    import dsl._
    def convert(order: Order) = for {
      quotes <- C .expect[Currency.Quotes](GET(Uri.unsafeFromString(s"http://943r6.mocklab.io/exchange-rates/${order.fromCurrency}"))) // uri interpolator doesn't support variables
    } yield quotes
      .get(order.toCurrency)
      .map(x => Offer(x, order.amount, x * order.amount))
  }
}
