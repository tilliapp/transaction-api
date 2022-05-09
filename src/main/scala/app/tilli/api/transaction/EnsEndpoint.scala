package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object EnsEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, String, ErrorResponse, EnsResolutionResponse, Any] = sttp.tapir.endpoint
    .get
    .in("ens" / path[String])
    .out(Serializer.jsonBody[EnsResolutionResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("ENS Resolution")

  def service: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(address: String): IO[Either[ErrorResponse, EnsResolutionResponse]] =
    Calls.ensResolution(address).asInstanceOf[IO[Either[ErrorResponse, EnsResolutionResponse]]]

}
