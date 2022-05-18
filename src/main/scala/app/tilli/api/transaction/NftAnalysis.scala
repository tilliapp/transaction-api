package app.tilli.api.transaction

import app.tilli.api.transaction.Calls.{formatter, getMarketData, getTimestamp}
import app.tilli.codec.TilliClasses.{ErrorResponse, ErrorResponseTrait, NftAsset, NftMarketData, NftSaleEvent}
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

  private val NonFungible: String = "non-fungible"

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

        collectionNftAssetOwner <- EitherT(database.getCollectionWithCodec[NftAsset](collectionNameNftAssetOwner).attempt).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        ownerAssets <- EitherT(getOwnerAssets(collectionSlug, uuid, collectionNftAssetOwner)(httpClient))
        _ = println(s"Finished nft assets $uuid! ($uuid ${getTimestamp()})")

        distinctNftSlugs <- EitherT(IO(Right(getDistinctNftCollections(ownerAssets)).asInstanceOf[Either[ErrorResponseTrait, List[NftMarketData]]]))

        collectionMarketData <- EitherT(database.getCollectionWithCodec[NftMarketData](collectionnNameMarketData).attempt).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        marketData <- EitherT(getMarketData(distinctNftSlugs, uuid, collectionMarketData))
        _ = println(s"Finished market data $uuid! ($uuid ${getTimestamp()})")

        enrichedOwnerAssets = enrichNftAssetsWithMarketData(ownerAssets, marketData)
          .filter(_.floorPrice.exists(_ >= 0.05))

        _ = println(s"Finished enriching owner assets $uuid! ($uuid ${getTimestamp()})")
        _ = println(s"Start writing file $uuid! ($uuid ${getTimestamp()})")
        write <- EitherT(writeToFile(enrichedOwnerAssets, uuid, collectionSlug)).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        _ = println(s"Finished writing file $uuid! ($uuid ${getTimestamp()})")
        _ = println(s"Done $uuid! ($uuid ${getTimestamp()})")
      } yield write
    chain
      .leftSemiflatTap(err => IO(s"Error: ${println(err.message)}"))
      .value
  }

  def getOwnerAssets(
    collectionSlug: String,
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftAsset]
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
    val chain =
      for {
        owners <- EitherT(getOwnersOfNftCollection(collectionSlug, uuid))
          .map(_.filter(_ != "0xb007d041fcde13439212976b2c798c2279b9642a")) // Opensea consistently returns 500xx
        _ = println(s"Got owners ($uuid ${getTimestamp()}) ${owners}")
        assets <- EitherT(getOwnerNFTAssets(owners, uuid, collectionCache))
      } yield assets
    chain.value
  }

  def getOwnersOfNftCollection(
    collectionSlug: String,
    uuid: UUID,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[String]]] = {

    // 1. Call events API to get token events for the collection
    // 2. a. filter all token events and keep only those that have winner_account.user.username not null
    // 		b. using that list of winner addresses filter the original token events for any events where seller.user.username==winner address
    //		c. Join those lists on the address.
    //		d. Sort by timestamp desc to get the most recent transaction for that address
    // 		e. If the most recent transaction is a sale then the user does not hold the token => drop the user, else keep the user as a holder

    val chain =
      for {
        allSales <- EitherT(Calls
          .getNftCollectionSaleEvents(collectionSlug, uuid)
          //          .map(_.map(_.filter(se => se.toAddress != se.fromAddress)))
        )
        //        _ = println(allSales.mkString("\n"))
        _ = println("All sales: " + allSales.size)
        _ = println("-----------------------------------------------------------------")
        tokens = countTokens(allSales)
        filteredTokens = tokens
          .filter(c => !c.ownerAddress.contains("0x68bb9fdd68c692def11f1351c73ee1af798540d4"))
          .filter(c => c.count.exists(_ > 0))
          .sortBy(_.count)
        distinctFiltered = filteredTokens.distinctBy(_.ownerAddress)
        //        _ = println(distinctFiltered.mkString("\n"))
        _ = println("Full set     = " + tokens.size)
        _ = println("Having > 0   = " + filteredTokens.size)
        _ = println("Distinct > 0 = " + distinctFiltered.size)

      } yield distinctFiltered.flatMap(_.ownerAddress).toList

    chain.value
  }

  def countTokens(sales: List[NftSaleEvent]): Seq[NftAsset] = {
    def toKey(e: NftSaleEvent): Option[(String, NftSaleEvent)] = e.toAddress.map(a => (s"${a}_${e.tokenId.getOrElse("NO_TOKEN_ID")}", e))

    def fromKey(e: NftSaleEvent): Option[(String, NftSaleEvent)] = e.fromAddress.map(a => (s"${a}_${e.tokenId.getOrElse("NO_TOKEN_ID")}", e))

    sales
      .sortBy(_.timestamp).reverse
      .filter(t => t.toAddress != t.fromAddress)
      .flatMap(e => List(toKey(e), fromKey(e)))
      .flatten
      .groupBy(_._1)
      .map { g =>
        def isCredit(a: NftSaleEvent) = g._1.startsWith(a.toAddress.getOrElse(""))

        def getSign(isCredit: Boolean): Int = if (isCredit) +1 else -1

        def sign(nse: NftSaleEvent): Int = getSign(isCredit(nse))

        g._2
          .map(_._2)
          .map(nse =>
            NftAsset(
              tokenId = nse.tokenId,
              count = nse.quantity.map(_ * sign(nse)),
              ownerAddress = if (sign(nse) < 0) nse.fromAddress else nse.toAddress,
              createdAt = nse.timestamp,
              updatedAt = Some(Instant.now),
            )
          )
          .reduce { (a, b) =>
            val totalCount = (a.count, b.count) match {
              case (Some(ac), Some(bc)) => Some(ac + bc)
              case (Some(_), None) => a.count
              case (None, Some(_)) => b.count
              case _ => None
            }
            val timestamp = (a.createdAt, b.createdAt) match {
              case (Some(ac), Some(bc)) => if (ac.isBefore(bc)) b.createdAt else a.createdAt
              case (Some(_), None) => a.createdAt
              case (None, Some(_)) => b.createdAt
              case _ => None
            }
            a.copy(
              count = totalCount,
              createdAt = timestamp,
              updatedAt = Some(Instant.now())
            )
          }
      }
      .toList
  }

  def getOwnerNFTAssets(
    owners: List[String],
    uuid: UUID,
    collectionCache: MongoCollection[IO, NftAsset]
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
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
    collectionCache: MongoCollection[IO, NftAsset],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
    // CAll getOwnerNFTAssetsFromCache and if empty call endpoint: Calls.getOwnerNFTAssets(owner, uuid)))
    val chain =
      for {
        ownersFromCache <- EitherT(getOwnerNFTAssetsFromCache(owner, collectionCache)).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
        owners <- EitherT(
          if (ownersFromCache.nonEmpty) IO(Right(ownersFromCache).asInstanceOf[Either[ErrorResponseTrait, List[NftAsset]]]) <* IO(println(s"getOwnerNFTAssetsFromCache cache hit: $owner ($uuid ${getTimestamp()})"))
          else Calls.getOwnerNFTAssets(owner, uuid, 0) <* IO(println(s"  getOwnerNFTAssetsFromCache cache miss: $owner ($uuid ${getTimestamp()})"))
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
    collectionCache: MongoCollection[IO, NftAsset],
  ): IO[Either[Throwable, List[NftAsset]]] = {
    collectionCache.find.filter(Filter.eq("ownerAddress", owner))
      .all
      .attempt
      .map(_.map(_.toList))
  }

  def insertIntoCache(
    collectionCache: MongoCollection[IO, NftAsset],
    nftAssetOwners: List[NftAsset],
  ): IO[Either[Throwable, Boolean]] = {
    if (nftAssetOwners.nonEmpty)
      collectionCache
        .insertMany(nftAssetOwners)
        .attempt
        .map(_.map(_.wasAcknowledged()))
    else IO(Right(false))
  }

  def getDistinctNftCollections(nftAssetOwners: List[NftAsset]): List[NftMarketData] =
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
      .distinctBy(_.collectionOpenSeaSlug.map(_.toLowerCase()))

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
    nftMarketData.collectionOpenSeaSlug
      .filter(s => s != null && s.nonEmpty)
      .map(slug =>
        collectionCache.find.filter(Filter.eq("collectionOpenSeaSlug", slug))
          .all
          .attempt
          .map(_.map(_.lastOption))
      ).getOrElse(
      IO(Left(new IllegalArgumentException("No slug provided")))
    )
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
    nftAssets: List[NftAsset],
    marketData: List[NftMarketData],
  ): List[NftAsset] = {
    val indexedMarketData = marketData
      .filter(_.collectionOpenSeaSlug.nonEmpty)
      .map(r => r.collectionOpenSeaSlug.get -> r)
      .toMap
    nftAssets.map { asset =>
      asset.collectionOpenSeaSlug
        .flatMap(slug => indexedMarketData.get(slug))
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
    nftAssetOwners: List[NftAsset],
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
        writer.writeAll(List(NftAsset.header) ++ nftAssetOwners.map(_.toStringList))
        writer.flush()
        writer.close()
        println(s"Finished write to $fileName")
      }.toEither
    }

}
