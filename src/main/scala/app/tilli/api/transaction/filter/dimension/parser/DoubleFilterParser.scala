package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.api.transaction.filter.dimension.validator.DoubleValidator

trait DoubleFilterParser
  extends NumberFilterParser[Double]
    with DoubleValidator