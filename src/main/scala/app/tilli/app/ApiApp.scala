package app.tilli.app

import app.tilli.api.transaction.TransactionApiHttpServer
import app.tilli.api.utils.{BlazeHttpClient, HttpClientConfig}
import cats.effect.{Async, ExitCode, IO, IOApp}

object ApiApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val async = Async[IO]

    val httpClientSettings = HttpClientConfig(
      connectTimeoutSecs = 30,
      requestTimeoutSecs = 30,
      maxRetryWaitMilliSecs = 60000,
      maxRetries = 20,
    )

    BlazeHttpClient
      .clientWithRetry(httpClientSettings)
      .use(client =>
        TransactionApiHttpServer(client)
      )


  }

}
