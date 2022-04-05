package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object ListEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, String, ErrorResponse, ListResponse, Any] = sttp.tapir.endpoint
    .get
    .in("list" / path[String])
    .out(Serializer.jsonBody[ListResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("List")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(listRequest: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, ListResponse]] =
    Calls.lists(listRequest)


}
