package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressBalanceEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, String, ErrorResponse, AddressBalanceResponse, Any] =
    sttp.tapir.endpoint
      .get
      .in("address" / path[String] / "balance")
      .out(Serializer.jsonBody[AddressBalanceResponse])
      .errorOut(Serializer.jsonBody[ErrorResponse])
      .name("Address Balance")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, AddressBalanceResponse]] = {
    Calls.addressBalance(input)
  }

}
