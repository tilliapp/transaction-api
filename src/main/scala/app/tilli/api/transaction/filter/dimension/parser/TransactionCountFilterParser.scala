package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait TransactionCountFilterParser extends NumberFilterParser {

  override val fieldName: String = "data.transactions"

  override val supportedDimension: codec.Dimension.Value = Dimension.transaction_count

}

object TransactionCountFilterParser extends TransactionCountFilterParser