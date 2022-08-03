package app.tilli.api.transaction.filter.dimension

import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

trait FilterParser[A] {

  def parse(filter: SimpleFilter): Either[Throwable, Filter] = {
    validateFilter(filter)
      .flatMap(validateValue)
      .flatMap(value => createFilter(filter, value))
  }

  def validateFilter(filter: SimpleFilter): Either[Throwable, SimpleFilter] = {
    if (filter.dimension != supportedDimension)
      Left(new IllegalStateException(s"${this.getClass.getSimpleName} was passed dimension ${filter.dimension} but expected ${supportedDimension}"))
    else if (!supportedOperators.contains(filter.operator))
      Left(new IllegalArgumentException(s"${this.getClass.getSimpleName} does not support operator ${filter.operator}. Supported operators are: ${supportedOperators.mkString(",")}"))
    else Right(filter)

  }

  def supportedDimension: Dimension.Value

  def supportedOperators: Set[Operator.Value]

  def validateValue(filter: SimpleFilter): Either[Throwable, A]

  protected def createFilter(simpleFilter: SimpleFilter, validatedValue: A): Either[Throwable, Filter]

}
