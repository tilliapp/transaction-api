package app.tilli.api.transaction.filter.dimension.validator

import app.tilli.codec.TilliClasses.SimpleFilter

import java.math.BigInteger
import scala.util.Try

trait AddressValidator extends Validator[String] {

  def cleanHexAddress(hexString: String): Either[Throwable, String] = {
    import cats.implicits._
    Try {
      val hex = if (hexString.startsWith("0x")) hexString.substring(2) else hexString
      val hexAgain = new BigInteger(hex, 16).toString(16)
      s"0x$hexAgain"
    }
      .toEither
      .leftMap(err => new IllegalArgumentException(s"A valid hex address is required", err))
  }

  override def validateValue(filter: SimpleFilter): Either[Throwable, String] = {
    Option(filter.value)
      .filter(s => s != null && s.nonEmpty)
      .map(_.toLowerCase())
      .filter(_.startsWith("0x"))
      .filter(_.length > 40)
      .toRight(new IllegalArgumentException(s"Malformed address: ${filter.value}"))
      .flatMap(cleanHexAddress)
  }

}
