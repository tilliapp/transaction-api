package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.api.transaction.filter.dimension.validator.{DoubleValidator, Validator}
import app.tilli.codec.Operator
import app.tilli.codec.TilliClasses.SimpleFilter
import mongo4cats.collection.operations.Filter

trait NumberFilterParser[A] extends FilterParser[A] with Validator[A] {

  def fieldName: String

  override val supportedOperators: Set[Operator.Value] = Set(
    Operator.lt,
    Operator.gt,
    Operator.eq,
  )

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: A,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.lt => Right(Filter.lt(fieldName, validatedValue))
      case Operator.gt => Right(Filter.gt(fieldName, validatedValue))
      case Operator.eq => Right(Filter.eq(fieldName, validatedValue))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}