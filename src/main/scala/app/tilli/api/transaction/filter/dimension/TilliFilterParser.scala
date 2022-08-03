package app.tilli.api.transaction.filter.dimension

import app.tilli.api.transaction.filter.dimension.TilliFilterParser.filters
import app.tilli.codec.Dimension
import app.tilli.codec.TilliClasses.{RequestFilters, SimpleFilter}
import cats.implicits._
import mongo4cats.collection.operations.Filter

trait TilliFilterParser {

  def parseFilters(apiRequestFilter: RequestFilters): Either[Throwable, Filter] = {
    apiRequestFilter.filters
      .map(f => parseFilter(f))
      .sequence
      .map(filters => filters.reduce((a, b) => a.and(b)))
  }

  def parseFilter(filter: SimpleFilter): Either[Throwable, Filter] =
    filters
      .get(filter.dimension)
      .map(_.parse(filter))
      .getOrElse(Left(new IllegalArgumentException(s"The filter configuration is not supported")))
}


object TilliFilterParser extends TilliFilterParser {

  val filters: Map[Dimension.Value, FilterParser[_]] = Map(
    Dimension.hold_time -> HoldTimeFilterParser,
    Dimension.address -> AddressFilterParser,
    Dimension.asset_contract_address -> AssetContractAddressFilterParser,
  )

}