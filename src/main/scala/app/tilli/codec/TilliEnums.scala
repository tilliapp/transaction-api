package app.tilli.codec

object AddressType extends Enumeration {
  val external, contract = Value
}

object TilliLabels extends Enumeration {
  val fraud, verified, safe = Value
}