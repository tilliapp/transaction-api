package app.tilli.app.config

import app.tilli.api.utils.HttpClientConfig
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

case class MongoDbConfig(
  user: String,
  password: String,
  host: String,
  protocol: String,
  config: Option[String],
  db: String,
) {

  def toMongoDbConfig: Either[Throwable, MongoDbConfigParsed] =
    toUrl.map(url =>
      MongoDbConfigParsed(
        url = url,
        db = db,
      ))

  def toUrl: Either[Throwable, String] = {
    val chain =
      for {
        user <- cleanOpt(user)
        password <- encodeOpt(password)
        protocol <- cleanOpt(protocol)
        config <- config.flatMap(cleanOpt)
        host <- cleanOpt(host)
      } yield {
        s"$protocol://$user:$password@$host/$config"
      }
    chain.toRight(new IllegalArgumentException("Could not construct MongoDb config"))
  }

  def cleanOpt(s: String): Option[String] =
    Option(s).filter(s => s != null && s.nonEmpty)

  def encodeOpt(s: String): Option[String] =
    cleanOpt(s).map(s => URLEncoder.encode(s, StandardCharsets.UTF_8.toString))
}

case class MongoDbConfigParsed(
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
