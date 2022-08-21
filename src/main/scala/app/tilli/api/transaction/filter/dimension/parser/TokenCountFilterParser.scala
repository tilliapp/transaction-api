package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.codec
import app.tilli.codec.Dimension

trait TokenCountFilterParser extends IntegerFilterParser {

  override val fieldName: String = "data.tokens"

  override val supportedDimension: codec.Dimension.Value = Dimension.token_count

}

object TokenCountFilterParser extends TokenCountFilterParser