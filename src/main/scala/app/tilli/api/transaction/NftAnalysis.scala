package app.tilli.api.transaction

import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.util.UUID

object NftAnalysis extends TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, String, ErrorResponse, NftAnalysisResponse, Any] = sttp.tapir.endpoint
    .get
    .in("nftAnalysis" / path[String])
    .out(Serializer.jsonBody[NftAnalysisResponse])
    .errorOut(Serializer.jsonBody[ErrorResponse])
    .name("NFT Analysis")

  def service(implicit
    httpClient: Client[IO]
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(q: String)(implicit
    httpClient: Client[IO],
  ): IO[Either[ErrorResponse, NftAnalysisResponse]] = {
    import cats.effect.unsafe.implicits.global
    val uuid = UUID.randomUUID()
    Calls.getNftAnalytics(q, uuid).unsafeRunAndForget()
    IO(Right(NftAnalysisResponse(uuid)))
  }


}
