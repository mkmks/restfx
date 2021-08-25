package org.mkmks.restfx

import cats.effect.Concurrent
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object RestfxRoutes {

  def convertRoutes[F[_]: Concurrent](C: Convert[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "convert" =>
        for {
          order <- req.as[Order]
          maybeOffer <- C.convert(order)
          response <- maybeOffer.fold(BadRequest("currency conversion failed for some unknown reason"))(Ok(_))
        } yield response
    }
  }

}
