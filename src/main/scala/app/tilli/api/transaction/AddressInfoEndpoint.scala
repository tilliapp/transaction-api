package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses.{AddressBalanceResponse, AddressInformationResponse, ErrorResponse}
import app.tilli.codec._
import cats.data.EitherT
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

object AddressInfoEndpoint extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, (String, Option[String]), ErrorResponse, AddressInformationResponse, Any] = sttp.tapir.endpoint
    .get
    .in("address" / path[String] / "info")
    .in(query[Option[String]]("filteredAddress"))
    .out(Serializer.jsonBody[AddressInformationResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("Address Information")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: (String, Option[String]))(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, AddressInformationResponse]] = {
    val receivingAddress = input._1.toLowerCase
    val sendingAddress = input._2.map(_.toLowerCase)

    val chain = for {
      volume <- EitherT(Calls.addressVolume(receivingAddress, sendingAddress)) // <* Temporal[IO].sleep(2.seconds))
      history <- EitherT(Calls.addressHistoryEtherscan(receivingAddress, sendingAddress, 10)) // <* Temporal[IO].sleep(2.seconds))
      nfts <- EitherT(Calls.addressNfts(receivingAddress)) // <* Temporal[IO].sleep(2.seconds))
      addressType <- EitherT(Calls.addressType(receivingAddress))
    } yield
      AddressInformationResponse(
        addressType,
        history,
        nfts,
        volume,
      )

    chain.value.asInstanceOf[IO[Either[ErrorResponse, AddressInformationResponse]]]
  }

}