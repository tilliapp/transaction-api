package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.api.transaction.filter.dimension.validator.IntegerValidator

trait IntegerFilterParser
  extends NumberFilterParser[Int]
    with IntegerValidator