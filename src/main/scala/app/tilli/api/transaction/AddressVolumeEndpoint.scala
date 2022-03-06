package app.tilli.api.transaction

import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import app.tilli.serializer.KeyConverter
import cats.effect.IO
import cats.implicits._
import org.http4s.client.Client
import org.http4s.{HttpRoutes, Uri}
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressVolumeEndpoint extends TilliCodecs with TilliSchema {

  object Serializer extends sttp.tapir.json.circe.TapirJsonCirce

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
    "https://api.etherscan.io/api?module=account&action=txlist&address=0xbecB05B9335fC0c53aEaB1C09733cdf9A0CdE85e&startblock=0&endblock=99999999&page=1&offset=10&sort=asc&apikey=2F4I4U42A674STIFNB4M522BRFSP8MHQHA"

    val receivingAddress = input._1.toLowerCase
    val sendingAddress = input._2.map(_.toLowerCase)

    val host = "https://api.etherscan.io"
    val apiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"
    val path = s"api"
    val startBlock = "0"
    val endblock = "99999999"
    val page = "1"
    val offset = "10000"
    val sort = "desc"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "txlist",
      "address" -> receivingAddress,
      "startblock" -> startBlock,
      "endblock" -> endblock,
      "page" -> page,
      "offset" -> offset,
      "sort" -> sort,
      "apikey" -> apiKey,
    )

    val Right(baseUri) = Uri.fromString(s"$host/$path")
    val uri = baseUri.withQueryParams(queryParams)

    httpClient.expectOr[String](uri) { err =>
      import cats.effect.unsafe.implicits.global
      val errorMessage = new String(err.body.compile.to(Array).unsafeRunSync())
      IO(println(errorMessage)) *> IO(new IllegalStateException("An error has occurred"))
    }
      .attempt
      .map(_
        .flatMap(s => KeyConverter.snakeCaseToCamelCase(s))
        .flatMap(s =>
          for {
            json <- io.circe.parser.parse(s)
            data <- json.as[EtherscanTransactions]
          } yield {
            val filteredData = sendingAddress match {
              case Some(address) => EtherscanTransactions(
                status = data.status,
                message = data.message,
                result = data.result.filter(r => r.from.toLowerCase == address.toLowerCase || r.to.toLowerCase == address.toLowerCase),
              )
              case None => data
            }
            AddressVolumeResponse(filteredData, receivingAddress)
          }
        ))
      .map(_.leftMap(e => ErrorResponse(e.getMessage)))

  }

}
