package org.mkmks.restfx

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    RestfxServer.stream[IO].compile.drain.as(ExitCode.Success)
}
