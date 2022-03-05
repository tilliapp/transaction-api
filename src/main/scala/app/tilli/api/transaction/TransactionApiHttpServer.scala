package app.tilli.api.transaction

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.openapi.circe.yaml.RichOpenAPI

object TransactionApiHttpServer {

  def apply(implicit httpClient: Client[IO]): IO[ExitCode] = {
    val port = 8080
    val interface = "0.0.0.0"

    val endpoints = Seq(
      AddressTypeEndpoint.endpoint,
      AddressNFTsEndpoint.endpoint,
    )

    val swaggerRoute = new SwaggerHttp4s(
      yaml = OpenAPIDocsInterpreter()
        .toOpenAPI(endpoints, title = "tilli API", version = "v1")
        .toYaml,
    ).routes[IO]


    val routes = List(
      AddressTypeEndpoint.service,
      AddressNFTsEndpoint.service,
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
