package app.tilli.api.transaction

import app.tilli.api.transaction.filter.FilterDbQuery
import app.tilli.api.transaction.filter.dimension.TilliFilterParser
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

object FilterEndpoint extends Logging with TilliCodecs with TilliSchema {

  val endpoint: Endpoint[Unit, RequestFilters, ErrorResponse, FilterResponse, Any] =
    sttp.tapir.endpoint
      .post
      .in("filter") // / path[String] / "balance")
      .in(Serializer.jsonBody[RequestFilters])
      .out(Serializer.jsonBody[FilterResponse])
      .errorOut(Serializer.jsonBody[ErrorResponse])
  //      .name("Address Balance")

  def service(implicit
    httpClient: Client[IO],
    analyticsTransactionCollection: MongoCollection[IO, TilliAnalyticsResultEvent],
  ): HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(endpoint.serverLogic(function))

  def function(input: RequestFilters)(implicit
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
