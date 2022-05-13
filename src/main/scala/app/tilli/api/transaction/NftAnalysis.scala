package app.tilli.api.transaction

import app.tilli.api.transaction.Calls.{formatter, getMarketData, getTimestamp}
import app.tilli.codec.TilliClasses.{ErrorResponse, ErrorResponseTrait, NftAssetOwner, NftMarketData}
import cats.data.EitherT
import cats.effect.{IO, Temporal}
import com.github.tototoshi.csv.CSVWriter
import mongo4cats.client.MongoClient
import mongo4cats.collection.MongoCollection
import mongo4cats.collection.operations.Filter
import mongo4cats.database.MongoDatabase
import org.http4s.client.Client

import java.io.File
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.Try

object NftAnalysis {

  private val PATTERN_FORMAT = "yyyy-MM-dd"
  private val formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

  def getTimestamp(now: Instant = Instant.now()): String = now.toString

  val mongoDatabaseName = "tilli"

  def analyze(
    collectionSlug: String,
    uuid: UUID,
  )(implicit
    httpClient: Client[IO],
    mongoClient: MongoClient[IO],
  ): IO[Either[ErrorResponseTrait, Unit]] = {
    println(s"Starting $uuid! ($collectionSlug $uuid ${getTimestamp()})")
    //    val distinctNftSlugs = List(NftMarketData(collectionOpenSeaSlug = Some("flyfrogs-ii8t6qt5t5")))
    //    val ownerAssets = List(NftAssetOwner(None, Some("0x79FCDEF22feeD20eDDacbB2587640e45491b757f"), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None))
    //    val distinctNftSlugs = List(NftMarketData(collectionOpenSeaSlug = Some("floor-app"), assetContractAddress = Some("0x79FCDEF22feeD20eDDacbB2587640e45491b757f")))
    val collectionNameNftAssetOwner = "nft_asset_owner"
    val collectionnNameMarketData = "nft_market_data"
    import io.circe.generic.auto._
    import mongo4cats.circe._

    val chain =
      for {
        database <- EitherT(mongoClient.getDatabase(mongoDatabaseName).attempt).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])

        collectionNftAssetOwner <- EitherT(database.getCollectionWithCodec[NftAssetOwner](collectionNameNftAssetOwner).attempt).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        ownerAssets <- EitherT(getOwnerAssets(collectionSlug, uuid, collectionNftAssetOwner)(httpClient))

        distinctNftSlugs <- EitherT(IO(Right(getDistinctNftCollections(ownerAssets)).asInstanceOf[Either[ErrorResponseTrait, List[NftMarketData]]]))

        collectionMarketData <- EitherT(database.getCollectionWithCodec[NftMarketData](collectionnNameMarketData).attempt).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        marketData <- EitherT(getMarketData(distinctNftSlugs, uuid, collectionMarketData))

        enrichedOwnerAssets = enrichNftAssetsWithMarketData(ownerAssets, marketData)
          .filter(_.floorPrice.exists(_ >= 0.05))
        write <- EitherT(writeToFile(enrichedOwnerAssets, uuid, collectionSlug)).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        _ = println(s"Done $uuid! ($uuid ${getTimestamp()})")
      } yield write
    chain
      .leftSemiflatTap(err => IO(s"Error: ${println(err.message)}"))
      .value
  }

  def getOwnerAssets(
    collectionSlug: String,
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftAssetOwner]
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAssetOwner]]] = {
    val chain =
      for {
        owners <- EitherT(Calls.getOwnersOfNftCollection(collectionSlug, uuid))
        _ = println(s"Got owners ($uuid ${getTimestamp()}) ${owners}")
        assets <- EitherT(getOwnerNFTAssets(owners, uuid, collectionCache))
      } yield assets
    chain.value
  }

  def getOwnerNFTAssets(
    owners: List[String],
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftAssetOwner]
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAssetOwner]]] = {
    import cats.implicits._
    val total = owners.size
    var counter = 1
    fs2.Stream
      .iterable(owners)
      .evalMap(owner => (IO(println(s"  getOwnerNFTAssetsFromCacheOrExternal($owner) ($uuid ${getTimestamp()}) $counter/$total")) *>
        IO {
          counter = counter + 1
        } *> getOwnerNFTAssetsFromCacheOrExternal(owner, uuid, collectionCache)))
      .takeWhile(_.isRight)
      .compile
      .toList
      .map(_.sequence)
      .map(_.map(_.flatten))
  }

  def getOwnerNFTAssetsFromCacheOrExternal(
    owner: String,
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftAssetOwner],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAssetOwner]]] = {
    // CAll getOwnerNFTAssetsFromCache and if empty call endpoint: Calls.getOwnerNFTAssets(owner, uuid)))
    val chain =
      for {
        ownersFromCache <- EitherT(getOwnerNFTAssetsFromCache(owner, uuid, collectionCache)).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        owners <- EitherT(
          if (ownersFromCache.nonEmpty) IO(Right(ownersFromCache).asInstanceOf[Either[ErrorResponseTrait, List[NftAssetOwner]]]) <* IO(println(s"getOwnerNFTAssetsFromCache cache hit: $owner ($uuid ${getTimestamp()})"))
          else Calls.getOwnerNFTAssets(owner, uuid) <* IO(println(s"  getOwnerNFTAssetsFromCache cache miss: $owner ($uuid ${getTimestamp()})"))
        )
        _ <- EitherT(
          if (ownersFromCache.isEmpty) insertIntoCache(collectionCache, owners)
          else IO(Right())
        ).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
      } yield owners
    chain.value
  }

  def getOwnerNFTAssetsFromCache(
    owner: String,
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftAssetOwner],
  ): IO[Either[Throwable, List[NftAssetOwner]]] = {
    collectionCache.find.filter(
      Filter.eq("ownerAddress", owner) &&
        Filter.ne("ownerAddress", "0x0000000000000000000000000000000000000000")
    )
      .all
      .attempt
      .map(_.map(_.toList))
  }

  def insertIntoCache(
    collectionCache: MongoCollection[IO, NftAssetOwner],
    nftAssetOwners: List[NftAssetOwner],
  ): IO[Either[Throwable, Boolean]] = {
    collectionCache
      .insertMany(nftAssetOwners)
      .attempt
      .map(_.map(_.wasAcknowledged()))
  }

  def getDistinctNftCollections(nftAssetOwners: List[NftAssetOwner]): List[NftMarketData] =
    nftAssetOwners.map(asset =>
      NftMarketData(
        assetContractAddress = asset.assetContractAddress,
        collectionOpenSeaSlug = asset.collectionOpenSeaSlug,
        numberOfOwners = asset.numberOfOwners,
        floorPrice = asset.floorPrice,
        averagePrice = asset.averagePrice,
        marketCap = asset.marketCap,
        totalVolume = asset.totalVolume,
      )
    ).filter(md => md.assetContractAddress.nonEmpty && md.collectionOpenSeaSlug.nonEmpty)
      .distinctBy(_.assetContractAddress.map(_.toLowerCase()))

  def getMarketData(
    nftCollections: List[NftMarketData],
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftMarketData],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftMarketData]]] = {
    import cats.implicits._
    val total = nftCollections.size
    var counter = 1
    fs2.Stream
      .iterable(nftCollections)
      .evalMap(nftCollection =>
        IO(println(s"getMarketDataFromCacheOrExternal(${nftCollection.collectionOpenSeaSlug.get}) ($uuid ${getTimestamp()}) $counter/$total")) *>
          IO {
            counter = counter + 1
          } *> getMarketDataFromCacheOrExternal(nftCollection, uuid, collectionCache)
      )
      .takeWhile(_.isRight)
      .compile
      .toList
      .map(_.sequence)
  }

  def getMarketDataFromCacheOrExternal(
    nftMarketData: NftMarketData,
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftMarketData],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, NftMarketData]] = {
    val chain =
      for {
        marketDataFromCache <- EitherT(getMarketDataFromCache(nftMarketData, uuid, collectionCache)).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        marketData <- EitherT(
          if (marketDataFromCache.nonEmpty) IO(Right(marketDataFromCache.get).asInstanceOf[Either[ErrorResponseTrait, NftMarketData]]) <* IO(println(s"  getMarketDataFromCache cache hit: ${nftMarketData.collectionOpenSeaSlug} ($uuid ${getTimestamp()})"))
          else Calls.getMarketData(nftMarketData) <* (IO(println(s"  getMarketDataFromCache cache miss: ${nftMarketData.collectionOpenSeaSlug} ($uuid ${getTimestamp()})")) <* Temporal[IO].sleep(300.milliseconds))
        )
        _ <- EitherT(
          if (marketDataFromCache.isEmpty) insertMarketDataIntoCache(collectionCache, marketData)
          else IO(Right())
        ).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
      } yield marketData
    chain.value
  }

  def getMarketDataFromCache(
    nftMarketData: NftMarketData,
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftMarketData],
  ): IO[Either[Throwable, Option[NftMarketData]]] = {
    collectionCache.find.filter(Filter.eq("assetContractAddress", nftMarketData.assetContractAddress.get))
      .all
      .attempt
      .map(_.map(_.lastOption))
  }

  def insertMarketDataIntoCache(
    collectionCache: MongoCollection[IO, NftMarketData],
    nftMarketData: NftMarketData,
  ): IO[Either[Throwable, Boolean]] = {
    collectionCache
      .insertOne(nftMarketData)
      .attempt
      .map(_.map(_.wasAcknowledged()))
  }


  def enrichNftAssetsWithMarketData(
    nftAssets: List[NftAssetOwner],
    marketData: List[NftMarketData],
  ): List[NftAssetOwner] = {
    val indexedMarketData = marketData.filter(_.assetContractAddress.nonEmpty).map(r => r.assetContractAddress.get -> r).toMap
    nftAssets.map { asset =>
      asset.assetContractAddress
        .map(address => indexedMarketData(address))
        .map(marketData =>
          asset.copy(
            numberOfOwners = marketData.numberOfOwners,
            floorPrice = marketData.floorPrice,
            averagePrice = marketData.averagePrice,
            marketCap = marketData.marketCap,
            totalVolume = marketData.totalVolume,
          )
        ).getOrElse(asset)
    }
  }

  def writeToFile(
    nftAssetOwners: List[NftAssetOwner],
    uuid: UUID,
    collectionName: String,
  ): IO[Either[Throwable, Unit]] =
    IO {
      val timestamp = formatter.format(Instant.now())
      val fileName = s"nft_assets_${collectionName}_${timestamp}_${uuid}.csv"

      Try {
        val f = new File(fileName)
        val writer = CSVWriter.open(f)
        println(s"Starting write to $fileName")
        writer.writeAll(List(NftAssetOwner.header) ++ nftAssetOwners.map(_.toStringList))
        writer.flush()
        writer.close()
        println(s"Finished write to $fileName")
      }.toEither
    }

}
