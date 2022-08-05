package app.tilli.api.transaction.filter.dimension

import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

import scala.util.Try

trait MintedFilterParser extends FilterParser[Boolean] {

  override val supportedDimension: codec.Dimension.Value = Dimension.minted

  override val supportedOperators: Set[Operator.Value] = Set(
    Operator.eq,
  )

  override def validateValue(filter: SimpleFilter): Either[Throwable, Boolean] = {
    Option(filter.value)
      .filter(s => s != null && s.nonEmpty)
      .toRight(new IllegalArgumentException(s"No boolean was provided"))
      .flatMap(s => Try(s.toBoolean).toEither)
  }

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: Boolean,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.eq => Right(Filter.eq("data.originatedFromNullAddress", validatedValue))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object MintedFilterParser extends MintedFilterParser
