package app.tilli.api.transaction.filter.dimension.validator

import app.tilli.codec.TilliClasses.SimpleFilter

import scala.util.Try

trait DoubleValidator extends Validator[Double] {

  override def validateValue(filter: SimpleFilter): Either[Throwable, Double] = {
    Try(java.lang.Double.parseDouble(filter.value))
      .toOption
      .filter(_ >= 0.0)
      .toRight(new IllegalArgumentException("Value must be a non negative double"))
  }

}
