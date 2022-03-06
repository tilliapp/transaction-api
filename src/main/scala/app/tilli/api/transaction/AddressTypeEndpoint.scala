package app.tilli.api.transaction

import app.tilli.codec.TilliClasses.{AddressTypeResponse, ErrorResponse}
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressTypeEndpoint extends TilliCodecs with TilliSchema {

  object Serializer extends sttp.tapir.json.circe.TapirJsonCirce

  val endpoint: Endpoint[Unit, String, ErrorResponse, AddressTypeResponse, Any] = sttp.tapir.endpoint
    .get
    .in("address" / path[String] / "type")
    .out(Serializer.jsonBody[AddressTypeResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("Address Type")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, AddressTypeResponse]] = {
    val address = input.toLowerCase
    Calls.addressType(address)
  }

}
