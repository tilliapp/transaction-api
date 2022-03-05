package app.tilli.api.transaction

import app.tilli.codec.TilliClasses.{AddressTypeResponse, ErrorResponse, EtherScanContract, MoralisNftTokenUri, MoralisNfts, Nft, NftsResponse}
import app.tilli.codec._
import app.tilli.serializer.KeyConverter
import cats.implicits._
import cats.effect.IO
import org.http4s.client.Client
import org.http4s.{Header, Headers, HttpRoutes, Request, Uri}
import org.typelevel.ci.CIString
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir._

import java.util.Base64

object AddressNFTsEndpoint extends TilliCodecs with TilliSchema {

  object Serializer extends sttp.tapir.json.circe.TapirJsonCirce

  import TilliHttp4sDecoders._

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
    val host = "https://deep-index.moralis.io"
    val apiKey = "gyk7fYMB0EOekZxvsLEDyE0Pm46H6py7iwn0x0fr7ortbcMPmUef0GPnHHtc8upP"
    val address = input
    val path = s"api/v2/$address/nft"
    val queryParams = Map(
      "chain" -> "eth",
    )

    val Right(baseUri) = Uri.fromString(s"$host/$path")
    val uri = baseUri.withQueryParams(queryParams)

    val request = Request[IO](
      uri = uri,
      headers = Headers(
        Header.Raw(CIString("X-Api-Key"), apiKey)
      )
    )

    httpClient.expectOr[String](request) { err =>
      import cats.effect.unsafe.implicits.global
      val errorMessage = new String(err.body.compile.to(Array).unsafeRunSync())
      IO(println(errorMessage)) *> IO(new IllegalStateException("An error has occurred"))
    }//.flatTap(d => IO(println(d)))
      .attempt
      .map(_
        .flatMap(s => KeyConverter.convert(s))
        .flatMap(s =>
          for {
            json <- io.circe.parser.parse(s)
            data <- json.as[MoralisNfts]
          } yield {
            NftsResponse(
              nfts = data.result.map(Nft(_)).toList
            )
          }
        ))
      .map(_.leftMap(e => ErrorResponse(e.getMessage)))
  }


}
