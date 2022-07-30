package app.tilli.app

import app.tilli.api.transaction.TransactionApiHttpServer
import app.tilli.api.utils.{BlazeHttpClient, HttpClientConfig}
import app.tilli.utils.ApplicationConfig
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import mongo4cats.client.MongoClient
import app.tilli.app.config.AppConfig.readerAppConfig
import app.tilli.app.config.Resources
import app.tilli.codec.TilliClasses.{AnalyticsResult, TilliAnalyticsResultEvent}

object ApiApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val async = Async[IO]

    import app.tilli.codec.MongoDbCodec._
    val resources = for {
      appConfig <- ApplicationConfig()
      httpClient <- BlazeHttpClient.clientWithRetry(appConfig.httpClientConfig)
      mongoDbConfig = appConfig.mongoDbConfig.toMongoDbConfig match {
        case Right(value) => value
        case Left (err) => throw err
      }
      mongoClient <- MongoClient.fromConnectionString(mongoDbConfig.url)
      mongoDatabase <- Resource.eval(mongoClient.getDatabase(appConfig.mongoDbConfig.db))
      analyticsTransactionCollection <- Resource.eval(mongoDatabase.getCollectionWithCodec[TilliAnalyticsResultEvent](appConfig.mongoDbCollectionAnalyticsTransaction))
    } yield Resources[IO](
      appConfig = appConfig,
      httpClient = httpClient,
      httpServerPort = appConfig.httpServerPort,
      analyticsTransactionCollection = analyticsTransactionCollection,
    )

    resources.use{r =>
      TransactionApiHttpServer(r.httpClient, r.analyticsTransactionCollection)
    }
  }

}
