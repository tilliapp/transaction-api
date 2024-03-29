package app.tilli.api.transaction.filter.dimension

import app.tilli.BaseSpec
import app.tilli.api.transaction.filter.dimension.parser.TilliFilterParser
import app.tilli.codec.TilliClasses.{FiltersRequest, SimpleFilter}
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

class HoldTimeFilterParserSpec extends BaseSpec {

  "HoldTimeFilter" must {

    "create filter" in {
      val filters = FiltersRequest(
        filters = Seq(
          SimpleFilter(
            dimension = Dimension.hold_time_avg,
            operator = Operator.lt,
            value = "4.0",
          )
        ),
        pageSize = Some(20),
        offset = Some(0),
        returnTotal = Some(false),
      )

      //      import app.tilli.codec.TilliCodecs._
      //      println(filters.asJson)

      val Right(result) = TilliFilterParser.parseFilters(filters)
      result.toString mustBe Filter.lt("data.holdTimeAvg", 4.0).toString
    }

  }

}
