package app.tilli.api.transaction.filter.dimension.parser

import app.tilli.BaseSpec
import app.tilli.codec.{Dimension, Operator}
import app.tilli.codec.TilliClasses.SimpleFilter
import mongo4cats.collection.operations.Filter

class IntegerFilterParserSpec extends BaseSpec {

  object IntegerFilterParser extends IntegerFilterParser {
    override def fieldName: String = "field"

    override def supportedDimension: Dimension.Value = Dimension.mint_count
  }

  "IntegerFilterParser" must {

    "fail to convert double to int" in {
      val simpleFilter = SimpleFilter(
        dimension =  Dimension.mint_count,
          operator = Operator.gt,
          value = "1.00",
      )
      IntegerFilterParser.parse(simpleFilter) mustBe a[Left[Throwable, Filter]]
    }

    "convert int to int" in {
      val simpleFilter = SimpleFilter(
        dimension =  Dimension.mint_count,
        operator = Operator.gt,
        value = "1",
      )
      IntegerFilterParser.parse(simpleFilter) mustBe a[Right[Throwable, Filter]]
    }

  }


}
