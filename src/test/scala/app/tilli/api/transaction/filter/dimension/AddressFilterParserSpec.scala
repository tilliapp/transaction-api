package app.tilli.api.transaction.filter.dimension

import app.tilli.BaseSpec
import app.tilli.api.transaction.filter.dimension.parser.{AddressFilterParser, TilliFilterParser}
import app.tilli.codec.TilliClasses.{FiltersRequest, SimpleFilter}
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

class AddressFilterParserSpec extends BaseSpec {

  "AddressFilter" must {

    "clean hex address" in {
      AddressFilterParser.cleanHexAddress("0xbecb05b9335fc0c53aeab1c09733cdf9a0cde85e") mustBe Right("0xbecb05b9335fc0c53aeab1c09733cdf9a0cde85e")
      AddressFilterParser.cleanHexAddress("0xbecB05B9335fC0c53aEaB1C09733cdf9A0CdE85e") mustBe Right("0xbecb05b9335fc0c53aeab1c09733cdf9a0cde85e")
      AddressFilterParser.cleanHexAddress("code injection<>") mustBe an[Left[IllegalArgumentException, String]]
    }

    "create filter" in {
      val filters = FiltersRequest(
        filters = Seq(
          SimpleFilter(
            dimension = Dimension.address,
            operator = Operator.eq,
            value = "0xbecb05b9335fc0c53aeab1c09733cdf9a0cde85e",
          )
        ),
        pageSize = Some(20),
        offset = Some(0),
        returnTotal = Some(false),
      )

      val Right(result) = TilliFilterParser.parseFilters(filters)
      result.toString mustBe Filter.eq("data.address", "0xbecb05b9335fc0c53aeab1c09733cdf9a0cde85e").toString
    }

  }

}
