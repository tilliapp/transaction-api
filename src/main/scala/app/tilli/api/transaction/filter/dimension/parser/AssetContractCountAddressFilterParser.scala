package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait AssetContractCountAddressFilterParser extends IntegerFilterParser {

  override val fieldName: String = "data.assetContractCount"

  override val supportedDimension: codec.Dimension.Value = Dimension.asset_contract_count

}

object AssetContractCountAddressFilterParser extends AssetContractCountAddressFilterParser

