package app.tilli.api.transaction.filter.dimension.validator

import app.tilli.codec.TilliClasses.SimpleFilter

trait Validator[A] {

  def validateValue(filter: SimpleFilter): Either[Throwable, A]

}



