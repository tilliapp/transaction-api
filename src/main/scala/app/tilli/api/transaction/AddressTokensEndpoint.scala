package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressTokensEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, String, ErrorResponse, AddressTokensResponse, Any] = sttp.tapir.endpoint
    .get
    .in("address" / path[String] / "tokens")
    .out(Serializer.jsonBody[AddressTokensResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("Address Tokens")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, AddressTokensResponse]] = {
    Calls.addressTokens(input).asInstanceOf[IO[Either[ErrorResponse, AddressTokensResponse]]]
  }

}
