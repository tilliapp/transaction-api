package app.tilli.api.transaction.filter.dimension

import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

trait AssetContractAddressFilterParser extends FilterParser[String] {

  override val supportedDimension: codec.Dimension.Value = Dimension.asset_contract_address

  override val supportedOperators: Set[Operator.Value] = AddressFilterParser.supportedOperators

  override def validateValue(filter: SimpleFilter): Either[Throwable, String] = AddressFilterParser.validateValue(filter)

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: String,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.eq => Right(Filter.eq("data.assetContractAddress", validatedValue))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object AssetContractAddressFilterParser extends AssetContractAddressFilterParser