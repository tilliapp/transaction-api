package app.tilli.api.transaction.filter

import app.tilli.api.transaction.filter.dimension.parser.TilliFilterParser
import app.tilli.api.utils.ApiSerdes.Serializer
import app.tilli.codec.TilliClasses.{ErrorResponse, FilterResponse, FiltersRequest, TilliAnalyticsResultEvent}
import app.tilli.codec.{TilliCodecs, TilliSchema}
import app.tilli.logging.Logging
import cats.effect.IO
import mongo4cats.collection.MongoCollection
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.implicits._

object FilterEndpoint extends Logging with TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, FiltersRequest, ErrorResponse, FilterResponse, Any] =
    sttp.tapir.endpoint
      .post
      .in("filter") // / path[String] / "balance")
      .in(Serializer.jsonBody[FiltersRequest])
      .out(Serializer.jsonBody[FilterResponse])
      .errorOut(Serializer.jsonBody[ErrorResponse])
  //      .name("Address Balance")

  def service(implicit
    httpClient: Client[IO],
    analyticsTransactionCollection: MongoCollection[IO, TilliAnalyticsResultEvent],
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: FiltersRequest)(implicit
    httpClient: Client[IO],
    analyticsTransactionCollection: MongoCollection[IO, TilliAnalyticsResultEvent],
  ): IO[Either[ErrorResponse, FilterResponse]] = {

    val filterQuery = new FilterDbQuery[IO](analyticsTransactionCollection)
    IO(TilliFilterParser.parseFilters(input))
      .flatMap {
        case Left(err) => IO(Left(err))
        case Right(filter) =>
          filterQuery.processQuery(filter, input.pageSize, input.offset, input.returnTotal.contains(true))
      }
      .map(_.map(a => FilterResponse(entries = a._2.map(_.data), total = a._1)))
      .map(_.leftMap {
        case ie: IllegalArgumentException => ErrorResponse(ie.getMessage)
        case _ => ErrorResponse("An error occurred while querying")
      })
  }


  // db.getSiblingDB("tilli").getCollection("analytics_transaction").aggregate([
  //  {
  //    $match: {$and: [{"data.duration": {$gt: 0}}, {"data.duration": {$lt: 5}}]}
  //  },
  //  {
  //    $sort: {"data.duration": -1}
  //  },
  //  {
  //    $project: {"address": "$data.address", "duration": "$data.duration", "_id": 0}
  //  },
  //  {
  //    $limit: 10
  //  }
  //])


  //  def holdTimeAveragePerAddress(): Unit = ???
  // db.getSiblingDB("tilli").getCollection("analytics_transaction").aggregate([
  //  {
  //    $group: {
  //      _id: {"data᎐address": "$data.address"},
  //      "avg(data_duration)": {$avg: "$data.duration"}
  //    }
  //  },
  //  {
  //    $project: {"address": "$_id.data᎐address", "avg": "$avg(data_duration)", "_id": 0}
  //  },
  //  {
  //    $limit: 10
  //  }
  //])


}
