package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressVolumeEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, (String, Option[String]), ErrorResponse, AddressVolumeResponse, Any] = sttp.tapir.endpoint
    .get
    .in("address" / path[String] / "volume")
    .in(query[Option[String]]("filteredAddress"))
    .out(Serializer.jsonBody[AddressVolumeResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("Address History")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: (String, Option[String]))(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, AddressVolumeResponse]] = {

    val receivingAddress = input._1.toLowerCase
    val sendingAddress = input._2.map(_.toLowerCase)

    Calls.addressVolume(receivingAddress, sendingAddress)

  }

}
