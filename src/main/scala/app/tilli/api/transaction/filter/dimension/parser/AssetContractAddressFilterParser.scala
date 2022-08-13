package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.api.transaction.filter.dimension.validator.AddressValidator
import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

trait AssetContractAddressFilterParser extends FilterParser[String] with AddressValidator {

  override val supportedDimension: codec.Dimension.Value = Dimension.asset_contract_address

  override val supportedOperators: Set[Operator.Value] = AddressFilterParser.supportedOperators

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: String,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.eq => Right(Filter.eq("data.assetContracts", validatedValue))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object AssetContractAddressFilterParser extends AssetContractAddressFilterParser