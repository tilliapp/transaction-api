package app.tilli.api.transaction.filter

import app.tilli.codec.TilliClasses.TilliAnalyticsResultEvent
import cats.MonadThrow
import cats.effect.Sync
import cats.implicits._
import mongo4cats.collection.MongoCollection
import mongo4cats.collection.operations.{Aggregate, Filter, Sort}

import scala.concurrent.duration.Duration

class FilterDbQuery[F[_] : Sync](
  analyticsTransactionCollection: MongoCollection[F, TilliAnalyticsResultEvent],
  defaultPageSize: Int = 20
)(implicit
  F: MonadThrow[F],
) {

  def holdTimeIsLt(
    duration: Duration,
    pageSize: Option[Int] = None,
    offset: Option[Int] = None,
    returnTotal: Boolean = false,
  ): F[Either[Throwable, (Option[Long], Iterable[TilliAnalyticsResultEvent])]] = {

    val count: F[Option[Long]] = {
      if(returnTotal) analyticsTransactionCollection.count(Filter.lt("data.duration", duration.toDays)).map(count => Some(count))
      else Sync[F].pure(None)
    }

    val data = analyticsTransactionCollection
      .find(Filter.lt("data.duration", duration.toDays))
      .sort(Sort.asc("_id"))
      .limit(pageSize.getOrElse(defaultPageSize))
      .skip(offset.getOrElse(0))
      .all

    val chain =
      for {
        count <- count
        data <- data
      } yield (count, data)


    chain.attempt
  }


  def holdTimeIsGt(duration: Duration): Unit = ???

  def holdTimeIs(duration: Duration): Unit = ???

  def holdTime(duration: Duration): Unit = ???

  def hasMinted(hasMinted: Boolean): Unit = ???

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
