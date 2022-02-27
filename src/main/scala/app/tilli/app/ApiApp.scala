package app.tilli.app

import app.tilli.api.transaction.TransactionApiHttpServer
import cats.effect.{ExitCode, IO, IOApp}

object ApiApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    TransactionApiHttpServer()
  }

}
