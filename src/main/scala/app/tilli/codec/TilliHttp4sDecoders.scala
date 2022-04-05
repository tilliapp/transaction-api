package app.tilli.codec

import app.tilli.codec.TilliClasses._
import cats.effect.IO
import org.http4s.circe.jsonOf

object TilliHttp4sDecoders {

  import TilliCodecs._

  implicit lazy val etherScanContractHttp4sDecoder = jsonOf[IO, EtherscanContract]

}
