package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressNFTsEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, String, ErrorResponse, NftsResponse, Any] = sttp.tapir.endpoint
    .get
    .in("address" / path[String] / "nfts")
    .out(Serializer.jsonBody[NftsResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("Address NFTs")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, NftsResponse]] = {
    val address = input.toLowerCase
    Calls.addressNfts(address)
  }


}
