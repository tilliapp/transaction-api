package app.tilli.api.transaction

import app.tilli.api.transaction.filter.FilterEndpoint
import app.tilli.codec.TilliClasses.TilliAnalyticsResultEvent
import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import mongo4cats.collection.MongoCollection
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s

object TransactionApiHttpServer {

  def apply(implicit
    httpClient: Client[IO],
    collection: MongoCollection[IO, TilliAnalyticsResultEvent],
  ): IO[ExitCode] = {
    val port = 8080
    val interface = "0.0.0.0"

    val endpoints = Seq(
      AddressTypeEndpoint.endpoint,
      AddressNFTsEndpoint.endpoint,
      AddressHistoryEndpoint.endpoint,
      AddressVolumeEndpoint.endpoint,
      AddressInfoEndpoint.endpoint,
      AddressBalanceEndpoint.endpoint,
      AddressTokensEndpoint.endpoint,
      ListEndpoint.endpoint,
      EnsEndpoint.endpoint,
      TwitterHandleEndpoint.endpoint,
      FilterEndpoint.endpoint,
    )

    val swaggerRoute = new SwaggerHttp4s(
      yaml = OpenAPIDocsInterpreter()
        .toOpenAPI(endpoints, title = "tilli API", version = "v1")
        .toYaml,
    ).routes[IO]

    val routes = List(
      AddressTypeEndpoint.service,
      AddressNFTsEndpoint.service,
      AddressHistoryEndpoint.service,
      AddressVolumeEndpoint.service,
      AddressInfoEndpoint.service,
      AddressBalanceEndpoint.service,
      AddressTokensEndpoint.service,
      ListEndpoint.service,
      EnsEndpoint.service,
      TwitterHandleEndpoint.service,
      FilterEndpoint.service,
      swaggerRoute,
    ).reduce((a, b) => a <+> b)
    val router = Router("/va1/" -> routes).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(port, interface)
      .withHttpApp(router)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

}
