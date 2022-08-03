package app.tilli.api.transaction.filter.dimension

import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

import scala.concurrent.duration.Duration
import scala.util.Try

trait HoldTimeFilterParser extends FilterParser[Int] {

  override val supportedDimension: codec.Dimension.Value = Dimension.hold_time

  override val supportedOperators: Set[Operator.Value] = Set(
    Operator.lt,
    Operator.gt,
    Operator.eq,
  )

  override def validateValue(filter: SimpleFilter): Either[Throwable, Int] = {
    Try(Integer.parseInt(filter.value))
      .toOption
      .filter(_ >= 0)
      .toRight(new IllegalArgumentException("Hold time value must be a non negative integer"))
  }

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: Int,
  ): Either[Throwable, Filter] = {
    val days = Duration.create(validatedValue, java.util.concurrent.TimeUnit.DAYS).toDays
    simpleFilter.operator match {
      case Operator.lt => Right(Filter.lt("data.duration", days))
      case Operator.gt => Right(Filter.gt("data.duration", days))
      case Operator.eq => Right(Filter.eq("data.duration", days))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object HoldTimeFilterParser extends HoldTimeFilterParser