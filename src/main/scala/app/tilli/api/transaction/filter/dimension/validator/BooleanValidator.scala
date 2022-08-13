package app.tilli.api.transaction.filter.dimension.validator

import app.tilli.codec.TilliClasses.SimpleFilter

import scala.util.Try

trait BooleanValidator extends Validator[Boolean] {

  override def validateValue(filter: SimpleFilter): Either[Throwable, Boolean] = {
    Option(filter.value)
      .filter(s => s != null && s.nonEmpty)
      .toRight(new IllegalArgumentException(s"No boolean value was provided"))
      .flatMap(s => Try(s.toBoolean).toEither)
  }

}
