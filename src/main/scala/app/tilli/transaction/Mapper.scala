package app.tilli.transaction

import cats.data.EitherT
import cats.effect.IO
import cats.effect.IO.Delay
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.{Endpoint, Schema, stringBody}

object Mapper extends TilliCodecs with TilliSchema {

  object Serializer extends sttp.tapir.json.circe.TapirJsonCirce

  val endpoint: Endpoint[Unit, String, String, AsaResponse, Any] = sttp.tapir.endpoint
    .post
    .in("authorize")
    .in(stringBody)
    .out(Serializer.jsonBody[AsaResponse])
    .errorOut(stringBody)
    .name("Authorize transaction")

  def function(input: String): IO[Either[String, AsaResponse]] = {
    println(s"$input")
    IO(Right(
      AsaResponse(
        result = "APPROVED",
        token = "a560ab93-3773-4fba-a060-7a87b93d91a8",
        avs_result = "APPROVED",
        balance = Balance(
          amount = 123,
          available = 123,
        )
      )
    ))
  }

  val service = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))
}
