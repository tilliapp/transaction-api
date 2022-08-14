package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait MintCountFilterParser extends NumberFilterParser {

  override val fieldName: String = "data.mints"

  override val supportedDimension: codec.Dimension.Value = Dimension.mint_count

}

object MintCountFilterParser extends MintCountFilterParser