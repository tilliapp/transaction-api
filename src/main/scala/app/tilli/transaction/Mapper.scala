package app.tilli.transaction

import sttp.tapir.RawBodyType.StringBody
import sttp.tapir.stringBody
import sttp.tapir.server.http4s._

object Mapper {

  val endpoint = sttp.tapir.endpoint
    .post
    .in("authorize")
    .out(stringBody)
    .errorOut(stringBody)
    .name("Authorize transaction")

  val service = endpoint.toRoutes(endpoint)
}
