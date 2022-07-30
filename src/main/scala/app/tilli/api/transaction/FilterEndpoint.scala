package app.tilli.api.transaction

import app.tilli.api.transaction.filter.FilterDbQuery
import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses._
import app.tilli.codec._
import app.tilli.logging.Logging
import cats.effect.IO
import cats.implicits._
import mongo4cats.collection.MongoCollection
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter

import scala.concurrent.duration.Duration

object FilterEndpoint extends Logging with TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, (Int, Option[Int]), ErrorResponse, FilterResponse, Any] =
    sttp.tapir.endpoint
      .get
      .in("filter") // / path[String] / "balance")
      .in(query[Int]("duration"))
      .in(query[Option[Int]]("skip"))
      .out(Serializer.jsonBody[FilterResponse])
      .errorOut(Serializer.jsonBody[ErrorResponse])
  //      .name("Address Balance")

  def service(implicit
    httpClient: Client[IO],
    analyticsTransactionCollection: MongoCollection[IO, TilliAnalyticsResultEvent],
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: (Int, Option[Int]))(implicit
    httpClient: Client[IO],
    analyticsTransactionCollection: MongoCollection[IO, TilliAnalyticsResultEvent],
  ): IO[Either[ErrorResponse, FilterResponse]] = {

    val query = new FilterDbQuery[IO](
      analyticsTransactionCollection
    )

    val duration = Duration.create(input._1, java.util.concurrent.TimeUnit.DAYS)
    query
      .holdTimeIsLt(duration, page = input._2)
      .flatTap {
        case Left(err) => IO(log.error("An error occurred while querying for holdTimeIsLt", err))
        case Right(_) => IO.unit
      }
      .map(_.map(a => FilterResponse(addresses = a.map(_.data))))
      .map(_.leftMap(_ => ErrorResponse("An error occurred while querying")))

  }

}
