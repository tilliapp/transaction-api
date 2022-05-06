package app.tilli.api.utils

import app.tilli.codec.TilliClasses.ErrorResponse
import app.tilli.logging.Logging
import app.tilli.serializer.KeyConverter
import cats.effect.IO
import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Headers, Request, Uri}

import scala.concurrent.duration.DurationInt

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

  def callPaged[A, B](
    host: String,
    path: String,
    pageParamKey: String,
    cursorQueryParamKey: String,
    queryParams: Map[String, String],
    conversion: A => B,
    headers: Headers = Headers.empty,
  )(implicit
    client: Client[IO],
    decoder: Decoder[A],
    encoder: Encoder[B],
  ): IO[List[Either[ErrorResponse, B]]] = {
    val stream =
      fs2.Stream.unfoldLoopEval(
        s = ""
      )(page => {
        import io.circe.optics.JsonPath.root
        import io.circe.syntax.EncoderOps
        import cats.effect.Temporal
        val withPageMap = if (page != null && page.nonEmpty) queryParams ++ Map(cursorQueryParamKey -> page) else queryParams
        call(host, path, withPageMap, conversion, headers)
          .map { r =>
            val obj = r
            val nextPageOption = r match {
              case Left(_) => None
              case Right(b) =>
                val json = b.asJson
                val nextPage = root.selectDynamic(pageParamKey).string.getOption(json)
                nextPage
            }
            println(s"Next page=$nextPageOption")
            (obj, nextPageOption)
          } <* Temporal[IO].sleep(250.milliseconds)
      })
    stream
      .takeWhile(r => r.isRight)
      .compile
      .toList
  }

}
