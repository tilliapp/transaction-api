package app.tilli.api.transaction.filter.dimension.validator

import app.tilli.codec.TilliClasses.SimpleFilter

import scala.util.Try

trait IntegerValidator extends Validator[Int] {

  override def validateValue(filter: SimpleFilter): Either[Throwable, Int] = {
    Try(Integer.parseInt(filter.value))
      .toOption
      .filter(_ >= 0)
      .toRight(new IllegalArgumentException("Value must be a non negative integer"))
  }

}
