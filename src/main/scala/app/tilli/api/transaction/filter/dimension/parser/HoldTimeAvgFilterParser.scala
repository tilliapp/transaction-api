package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait HoldTimeAvgFilterParser extends NumberFilterParser {

  override val fieldName: String = "data.holdTimeAvg"

  override val supportedDimension: codec.Dimension.Value = Dimension.hold_time_avg

}

object HoldTimeAvgFilterParser extends HoldTimeAvgFilterParser