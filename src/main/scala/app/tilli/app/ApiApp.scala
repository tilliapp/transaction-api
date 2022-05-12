package app.tilli.app

import app.tilli.api.transaction.TransactionApiHttpServer
import app.tilli.api.utils.{BlazeHttpClient, HttpClientConfig}
import app.tilli.persistence.MongoDbAdapter
import cats.effect.{Async, ExitCode, IO, IOApp}
import mongo4cats.client.MongoClient
import org.http4s.client.Client

case class Resources(
  httpClient: Client[IO],
  mongoDbClient: MongoClient[IO],
)

object ApiApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val async = Async[IO]

    val httpClientSettings = HttpClientConfig(
      connectTimeoutSecs = 30,
      requestTimeoutSecs = 30,
      maxRetryWaitMilliSecs = 60000,
      maxRetries = 20,
    )

    val url = "mongodb://localhost:27017"
    val mongoDbResource = new MongoDbAdapter[IO](url)
    val resources = for {
      httpClient <- BlazeHttpClient.clientWithRetry(httpClientSettings)
      mongoDbClient <- mongoDbResource.resource

    } yield Resources(httpClient, mongoDbClient)

    resources.use(r =>
      TransactionApiHttpServer(r.httpClient, r.mongoDbClient)
    )
  }

}
