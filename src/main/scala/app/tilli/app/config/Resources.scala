package app.tilli.app.config

import app.tilli.codec.TilliClasses.TilliAnalyticsResultEvent
import mongo4cats.collection.MongoCollection
import org.http4s.client.Client

case class Resources[F[_]](
  appConfig: AppConfig,
  httpClient: Client[F],
  httpServerPort: Int,
  analyticsTransactionCollection: MongoCollection[F, TilliAnalyticsResultEvent],
)