package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait HoldTimeMinFilterParser extends DoubleFilterParser {

  override val fieldName: String = "data.holdTimeMin"

  override val supportedDimension: codec.Dimension.Value = Dimension.hold_time_min

}

object HoldTimeMinFilterParser extends HoldTimeMinFilterParser