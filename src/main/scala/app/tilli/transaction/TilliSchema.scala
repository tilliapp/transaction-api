package app.tilli.transaction

import sttp.tapir.Schema

trait TilliSchema {

  import io.circe.generic.auto._

  implicit lazy val BalanceSchema: Schema[Balance] = Schema.derived
  implicit lazy val AsaResponseSchema: Schema[AsaResponse] = Schema.derived

}

object TilliSchema extends TilliSchema
