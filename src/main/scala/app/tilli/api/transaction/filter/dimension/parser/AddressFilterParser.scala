package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.api.transaction.filter.dimension.validator.AddressValidator
import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

trait AddressFilterParser extends FilterParser[String] with AddressValidator {

  override val supportedDimension: codec.Dimension.Value = Dimension.address

  override val supportedOperators: Set[Operator.Value] = Set(
    Operator.eq,
  )

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: String,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.eq => Right(Filter.eq("data.address", validatedValue))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object AddressFilterParser extends AddressFilterParser