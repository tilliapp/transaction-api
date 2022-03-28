package app.tilli.api.utils

import app.tilli.codec.TilliClasses.ErrorResponse
import app.tilli.logging.Logging
import app.tilli.serializer.KeyConverter
import cats.effect.IO
import cats.implicits._
import io.circe.Decoder
import org.http4s.client.Client
import org.http4s.{Headers, Request, Uri}

object SimpleHttpClient extends Logging {

  def call[A, B](
    host: String,
    path: String,
    queryParams: Map[String, String],
    conversion: A => B,
    headers: Headers = Headers.empty,
  )(implicit
    client: Client[IO],
    decoder: Decoder[A],
  ): IO[Either[ErrorResponse, B]] = {
    val Right(baseUri) = Uri.fromString(s"$host/$path")
    val uri = baseUri.withQueryParams(queryParams)

    val call = {
      if (headers.isEmpty) client.expectOr[String](uri) _
      else client.expectOr[String](Request[IO](uri = uri, headers = headers)) _
    }

    call { err =>
      import cats.effect.unsafe.implicits.global
      val errorMessage = new String(err.body.compile.to(Array).unsafeRunSync()) + s": ${uri.renderString}"
      IO(log.error(errorMessage)) *> IO(new IllegalStateException("An error has occurred"))
    }
      .attempt
      .map(_
        .flatMap(s => KeyConverter.snakeCaseToCamelCase(s))
        .flatMap(s =>
          for {
            json <- io.circe.parser.parse(s)
            data <- json.as[A]
          } yield conversion(data)
        ))
      .map(_.leftMap(e => ErrorResponse(e.getMessage)))
  }

}
