package app.tilli.api.transaction.filter

import app.tilli.codec.TilliClasses.TilliAnalyticsResultEvent
import cats.MonadThrow
import cats.effect.Sync
import cats.implicits._
import mongo4cats.collection.MongoCollection
import mongo4cats.collection.operations.{Filter, Sort}

class FilterDbQuery[F[_] : Sync](
  analyticsTransactionCollection: MongoCollection[F, TilliAnalyticsResultEvent],
  defaultPageSize: Int = 20
)(implicit
  F: MonadThrow[F],
) {

  def processQuery(
    filter: Filter,
    pageSize: Option[Int] = None,
    offset: Option[Int] = None,
    returnTotal: Boolean = false,
  ): F[Either[Throwable, (Option[Long], Iterable[TilliAnalyticsResultEvent])]] = {

    val count: F[Option[Long]] = {
      if (returnTotal) analyticsTransactionCollection.count(filter).map(count => Some(count))
      else Sync[F].pure(None)
    }

    val data = analyticsTransactionCollection
      .find(filter)
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
}
