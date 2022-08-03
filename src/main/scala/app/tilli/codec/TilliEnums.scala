package app.tilli.codec

object AddressType extends Enumeration {
  val external, contract = Value
}

object TilliLabels extends Enumeration {
  val fraud, verified, safe = Value
}

object Dimension extends Enumeration {
  val hold_time, address, asset_contract_address = Value
}

object Operator extends Enumeration {
  val lt, gt, eq  = Value
}