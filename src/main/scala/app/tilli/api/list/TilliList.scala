package app.tilli.api.list

import app.tilli.codec.TilliClasses.ListEntry
import app.tilli.codec.{TilliCodecs, TilliLabels}
import cats.data.EitherT
import cats.effect.IO
import io.circe.Codec

import scala.io.Source
import scala.util._

object TilliList {

  private implicit val decoder: Codec[ListEntry] = TilliCodecs.listEntryCodec

  private[list] def loadTilliBlockList: Either[Throwable, List[ListEntry]] =
    loadFile("data/block.json")
      .map(addLabel(_, TilliLabels.fraud.toString))

  private[list] def loadTilliTokenList: Either[Throwable, List[ListEntry]] =
    loadFile("data/token.json")
      .map(addLabel(_, TilliLabels.safe.toString))

  private[list] def loadTilliVerifiedList: Either[Throwable, List[ListEntry]] =
    loadFile("data/verified.json")
      .map(addLabel(_, TilliLabels.verified.toString))

  lazy val tilliBlockList = EitherT(IO.delay(loadTilliBlockList))
  lazy val tilliTokenList = EitherT(IO.delay(loadTilliTokenList))
  lazy val tilliVerifiedList = EitherT(IO.delay(loadTilliVerifiedList))

  lazy val tilliList: EitherT[IO, Throwable, List[ListEntry]] = for {
    bl <- tilliBlockList
    tl <- tilliTokenList
    tv <- tilliVerifiedList
  } yield bl ++ tl ++ tv

  private def addLabel(entries: List[ListEntry], label: String): List[ListEntry] =
    entries.map(_.copy(labels = Some(List(label))))

  def loadFile(fileName: String): Either[Throwable, List[ListEntry]] =
        Try(Source.fromResource(fileName).mkString)
          .toEither
          .flatMap(data =>
            io.circe.parser.parse(data)
              .flatMap(json => json.as[List[ListEntry]].asInstanceOf[Either[Throwable, List[ListEntry]]])
          )
}
