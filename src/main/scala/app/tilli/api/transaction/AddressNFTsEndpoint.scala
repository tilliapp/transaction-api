package app.tilli.api.transaction

import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import app.tilli.serializer.KeyConverter
import cats.implicits._
import cats.effect.IO
import org.http4s.client.Client
import org.http4s.{Header, Headers, HttpRoutes, Request, Uri}
import org.typelevel.ci.CIString
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir._

object AddressNFTsEndpoint extends TilliCodecs with TilliSchema {

  object Serializer extends sttp.tapir.json.circe.TapirJsonCirce

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
