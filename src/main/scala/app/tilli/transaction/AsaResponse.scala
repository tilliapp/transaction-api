package app.tilli.transaction

// {
//  "result": String,
//  "token": String,
//  "avs_result": String,
//  "balance": {
//      "amount": Integer,
//      "available": Integer
//  }
//}

case class Balance(
  amount: Int,
  available: Int,
)

case class AsaResponse(
  result: String,
  token: String,
  avs_result: String,
  balance: Balance,
)
