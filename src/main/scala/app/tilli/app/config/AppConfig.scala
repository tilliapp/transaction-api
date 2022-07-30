package app.tilli.app.config

import app.tilli.api.utils.HttpClientConfig
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class MongoDbConfig(
  url: String,
  db: String,
)

case class AppConfig(
  environment: String,
  httpServerPort: Int,
  httpClientConfig: HttpClientConfig,

  mongoDbConfig: MongoDbConfig,
  mongoDbCollectionAnalyticsTransaction: String,
)

object AppConfig {

  implicit val readerHttpClientConfig: ConfigReader[HttpClientConfig] = deriveReader[HttpClientConfig]
  implicit val readerMongoDbConfig: ConfigReader[MongoDbConfig] = deriveReader[MongoDbConfig]
  implicit val readerAppConfig: ConfigReader[AppConfig] = deriveReader[AppConfig]

}
