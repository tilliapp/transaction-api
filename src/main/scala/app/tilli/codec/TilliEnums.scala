package app.tilli.codec

object AddressType extends Enumeration {
  val external, contract = Value
}

object TilliLabels extends Enumeration {
  val fraud, verified, safe = Value
}

object Dimension extends Enumeration {
  val
  address,
  asset_contract_address,
  asset_contract_count,

  hold_time_avg,
  hold_time_max,
  hold_time_min,

  mint_count,
  transaction_count,
  token_count,

  has_minted
  = Value
}

object Operator extends Enumeration {
  val lt, gt, eq = Value
}