package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait HoldTimeMaxFilterParser extends NumberFilterParser {

  override val fieldName: String = "data.holdTimeMax"

  override val supportedDimension: codec.Dimension.Value = Dimension.hold_time_max

}

object HoldTimeMaxFilterParser extends HoldTimeMaxFilterParser