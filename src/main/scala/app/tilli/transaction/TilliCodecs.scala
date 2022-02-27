package app.tilli.transaction

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

trait TilliCodecs {

  implicit lazy val balanceCodec: Codec[Balance] = deriveCodec
  implicit lazy val asaResponseCodec: Codec[AsaResponse] = deriveCodec

}

object TilliCodecs extends TilliCodecs