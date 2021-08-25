package org.mkmks.restfx

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.implicits._

object RestfxServer {

  def stream[F[_]: Async]: Stream[F, Nothing] = {
    for {
      client <- Stream.resource(EmberClientBuilder.default[F].build)
      convertAlg = Convert.impl[F](client)

      httpApp = RestfxRoutes.convertRoutes[F](convertAlg).orNotFound
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- Stream.resource(
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
        Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
