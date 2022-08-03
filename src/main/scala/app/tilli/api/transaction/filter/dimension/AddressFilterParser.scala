package app.tilli.api.transaction.filter.dimension

import app.tilli.codec
import app.tilli.codec.TilliClasses.SimpleFilter
import app.tilli.codec.{Dimension, Operator}
import mongo4cats.collection.operations.Filter

import java.math.BigInteger
import scala.concurrent.duration.Duration
import scala.util.Try

trait AddressFilterParser extends FilterParser[String] {

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

  override val supportedDimension: codec.Dimension.Value = Dimension.address

  override val supportedOperators: Set[Operator.Value] = Set(
    Operator.eq,
  )

  override def validateValue(filter: SimpleFilter): Either[Throwable, String] = {
    Option(filter.value)
      .filter(s => s != null && s.nonEmpty)
      .map(_.toLowerCase())
      .filter(_.startsWith("0x"))
      .filter(_.length > 40)
      .toRight(new IllegalArgumentException(s"Malformed address: ${filter.value}"))
      .flatMap(cleanHexAddress)
  }

  override protected def createFilter(
    simpleFilter: SimpleFilter,
    validatedValue: String,
  ): Either[Throwable, Filter] = {
    simpleFilter.operator match {
      case Operator.eq => Right(Filter.eq("data.address", validatedValue))
      case op => Left(new IllegalStateException(s"Operator $op not supported for ${this.getClass.getSimpleName}"))
    }
  }
}

object AddressFilterParser extends AddressFilterParser