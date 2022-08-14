package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.api.transaction.filter.dimension.validator.BooleanValidator
import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

trait MintedFilterParser extends FilterParser[Boolean] with BooleanValidator {

  override val supportedDimension: codec.Dimension.Value = Dimension.has_minted

  override val supportedOperators: Set[Operator.Value] = Set(
    Operator.eq,
  )

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: Boolean,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.eq => Right(Filter.gt("data.mints", 0))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object MintedFilterParser extends MintedFilterParser
