package app.tilli.codec

import app.tilli.codec.TilliClasses.{Header, Origin, TilliAnalyticsResultEvent}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import mongo4cats.circe.MongoJsonCodecs

object MongoDbCodec extends TilliCodecs with MongoJsonCodecs {
  // Note: it's important that the last mixin is MongoJsonCodecs since it needs to provide a custom codec for dates etc. for mongo data

  // Tilli Events
  implicit lazy val codecOrigin: Codec[Origin] = deriveCodec
  implicit lazy val codecHeader: Codec[Header] = deriveCodec
  implicit lazy val codecTilliAnalyticsResultEvent: Codec[TilliAnalyticsResultEvent] = deriveCodec

}