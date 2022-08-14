package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec.Dimension
import app.tilli.codec.TilliClasses.{FiltersRequest, SimpleFilter}
import cats.implicits._
import mongo4cats.collection.operations.Filter

trait TilliFilterParser {

  def parseFilters(apiRequestFilter: FiltersRequest): Either[Throwable, Filter] = {
    apiRequestFilter.filters
      .map(f => parseFilter(f))
      .sequence
      .map(filters => filters.reduce((a, b) => a.and(b)))
  }

  def parseFilter(filter: SimpleFilter): Either[Throwable, Filter] =
    TilliFilterParser
      .filters
      .get(filter.dimension)
      .map(_.parse(filter))
      .getOrElse(Left(new IllegalArgumentException(s"The filter configuration is not supported")))
}

object TilliFilterParser extends TilliFilterParser {

  val filters: Map[Dimension.Value, FilterParser[_]] = Map(
    Dimension.address -> AddressFilterParser,
    Dimension.asset_contract_address -> AssetContractAddressFilterParser,
    Dimension.asset_contract_count -> AssetContractCountAddressFilterParser,

    Dimension.hold_time_avg -> HoldTimeAvgFilterParser,
    Dimension.hold_time_min -> HoldTimeMinFilterParser,
    Dimension.hold_time_max -> HoldTimeMaxFilterParser,

    Dimension.mint_count -> MintCountFilterParser,
    Dimension.transaction_count -> TransactionCountFilterParser,
    Dimension.token_count -> TokenCountFilterParser,

    Dimension.has_minted -> MintedFilterParser,
  )

}