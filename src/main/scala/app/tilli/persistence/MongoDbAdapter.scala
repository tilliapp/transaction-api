package app.tilli.persistence

import cats.effect.{Async, IO, IOApp, Resource}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import mongo4cats.client.MongoClient
import mongo4cats.collection.operations.{Filter, Update}
import mongo4cats.bson.{Document, ObjectId}
import mongo4cats.collection.MongoCollection
import mongo4cats.database.MongoDatabase

import java.time.Instant
import java.util.UUID

class MongoDbAdapter[F[_] : Async](
  url: String = "mongodb://localhost:27017",
) {

  def resource: Resource[F, MongoClient[F]] = MongoClient.fromConnectionString[F](url)

}

object MongoDbAdapter extends IOApp.Simple {

  //  override val run: IO[Unit] =
  //    MongoClient.fromConnectionString[IO]("mongodb://localhost:27017").use { client =>
  //      for {
  //        db <- client.getDatabase("testdb")
  //        coll <- db.getCollection("docs")
  ////        _ <- coll.insertMany((0 to 100).map(i => Document("name" -> s"doc-$i-${i.toChar}", "index" -> i)))
  //        //        _ <- coll.insertMany((0 to 100).map(i => Document("name" -> s"doc-$i", "index" -> i)))
  //        //        _    <- coll.insertMany((0 to 100).map(i => Document("name" -> s"doc-$i", "index" -> i)))
  //        docs <- coll.find
  //          .filter(Filter.eq("id","6279a27cc3293a51d17a588a"))
  ////          .filter(Filter.gte("index", 10) && Filter.regex("name", "doc-[1-9]0-"))
  //          .sortByDesc("name")
  //          .limit(5)
  //          .all
  //        _ <- IO.println(docs.mkString(",\n"))
  //      } yield ()
  //    }

  override val run: IO[Unit] = {
    val url = "mongodb://localhost:27017"
    val resource = new MongoDbAdapter[IO](url).resource
    val dbName = "test_docs"
    val collectionName = "docs"
    resource.use { client =>
      for {
        db <- client.getDatabase(dbName)
        _ <- createCollectionWithData(db,collectionName)
        collection <- getCollection(db, collectionName)
        _ <- printCollection(collection)
//        updated <- updateDocument(ObjectId("627baf6963ee185589f1c1ea"), collection)
//        _ <- IO(println(s"Updated: ${updated.nonEmpty}"))
//        _ <- printCollection(collection)
      } yield ()
    }
  }
  // Guide: https://boristheastronaut.medium.com/scala-mongodb-and-cats-effect-1d6875e973fe

  import io.circe.generic.auto._
  import mongo4cats.collection.MongoCollection
  import mongo4cats.bson.ObjectId
  import mongo4cats.circe._

  case class MyDoc(
    _id: ObjectId,
    addressId: UUID,
    name: Option[String],
    index: Int,
    createdAt: Instant,
    updateAt: Instant,
  )

//  implicit lazy val myDocCodec: Codec[MyDoc] = deriveCodec

  def createCollectionWithData(
    db: MongoDatabase[IO],
    collectionName: String,
    now: Instant = Instant.now(),
  ): IO[Unit] = {
    for {
      coll <- db.getCollectionWithCodec[MyDoc](collectionName)
      docs = (1 to 100).map(i => MyDoc(ObjectId(), UUID.randomUUID(), Some(s"doc-$i-${i.toChar}"), i, now, now)).toList
      _ <- coll.insertMany(docs)
    } yield ()
  }
  def getCollection(
    db: MongoDatabase[IO],
    collectionName: String,
  ): IO[MongoCollection[IO, MyDoc]] = {
    db.getCollectionWithCodec[MyDoc](collectionName)
    //      .flatTap(collection => collection.find.all)
  }

  def printCollection(collection: MongoCollection[IO, MyDoc]): IO[Unit] = {
    collection.find.limit(50).all
      .map(doc => doc.toList.map(r => s"$r").mkString(","))
      .map(println(_))
  }

  def updateDocument(
    id: ObjectId,
    collection: MongoCollection[IO, MyDoc],
  ): IO[Option[MyDoc]] = {
    val filter = Filter.idEq(id)
    val updateName = Update
      .set("name", "Updated Name")
      .currentTimestamp("updatedAt")

    for {
      found <- collection.findOneAndUpdate(
        filter,
        updateName
      )
    } yield found
  }

}
