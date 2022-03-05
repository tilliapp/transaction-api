package app.tilli.api.transaction

import app.tilli.codec.TilliClasses.{AddressTypeResponse, EtherScanContract}
import app.tilli.codec._
import cats.effect.IO
import org.http4s.{HttpRoutes, Uri}
import org.http4s.client.Client
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.{Endpoint, Schema, path, stringBody}
import sttp.tapir._

object AddressTypeEndpoint extends TilliCodecs with TilliSchema {

  object Serializer extends sttp.tapir.json.circe.TapirJsonCirce

  import TilliCodecs._
  import TilliSchema._
  import TilliHttp4sDecoders._

  val endpoint: Endpoint[Unit, String, String, AddressTypeResponse, Any] = sttp.tapir.endpoint
    .get
    .in("address" / path[String] / "type")
    .out(Serializer.jsonBody[AddressTypeResponse])
    .errorOut(stringBody)
    .name("Address Type")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[String, AddressTypeResponse]] = {
    "https://api.etherscan.io/api?module=contract&action=getabi&address=0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2&apikey=gyk7fYMB0EOekZxvsLEDyE0Pm46H6py7iwn0x0fr7ortbcMPmUef0GPnHHtc8upP"
    val host = "https://api.etherscan.io"
    val apiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"
    //    val address = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    val address = input
    val path = s"api"
    val queryParams = Map(
      "module" -> "contract",
      "action" -> "getabi",
      "address" -> address,
      "apikey" -> apiKey,
    )

    val Right(baseUri) = Uri.fromString(s"$host/$path")
    val uri = baseUri.withQueryParams(queryParams)

    println(s"$input")
    httpClient.expectOr[EtherScanContract](uri) { e =>
      IO(new IllegalStateException("Error"))
    }
      .flatTap(d => IO(println(d)))
      .map(c =>
        Right(
          AddressTypeResponse(
            addressType = if (c.status == "1") AddressType.contract else AddressType.external
          )
        )
      )
  }

}
