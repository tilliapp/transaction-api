package app.tilli.api.transaction

import app.tilli.transaction.Mapper
import cats.effect.{ExitCode, IO}
import org.http4s.blaze.server.BlazeServerBuilder

object TransactionApiHttpServer {

  def apply(): IO[ExitCode] = {
    val port = 8080
    val interface = "0.0.0.0"

    val app = Mapper.service

    BlazeServerBuilder[IO]
      .bindHttp(port, interface)
      .withHttpApp(app.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

}
