package app.tilli.api.transaction

import app.tilli.api.list.TilliList
import app.tilli.api.transaction.Calls.getTimestamp
import app.tilli.api.utils.SimpleHttpClient
import app.tilli.codec.AddressType
import app.tilli.codec.TilliClasses._
import app.tilli.codec.TilliCodecs._
import cats.data.EitherT
import cats.effect.{IO, Temporal}
import com.github.tototoshi.csv.CSVWriter
import io.circe.Json
import io.circe.optics.JsonPath._
import org.http4s.client.Client
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIString
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

import java.io.File
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.Try

object Calls {

  // TODO: Add accept: application/json to all calls

  val etherScanHost = "https://api.etherscan.io"
  val etherScanApiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"

  val moralisHost = "https://deep-index.moralis.io"
  val moralisApiKey = "gyk7fYMB0EOekZxvsLEDyE0Pm46H6py7iwn0x0fr7ortbcMPmUef0GPnHHtc8upP"
  val moralisApiKeyHeader: Header.Raw = Header.Raw(CIString("X-Api-Key"), moralisApiKey)
  val moralisApiKeyHeaderInHeader: Headers = Headers(moralisApiKeyHeader)

  val coinGeckoHost = "https://api.coingecko.com"

  val twitterHost = "https://api.twitter.com"
  val twitterBearerToken = "AAAAAAAAAAAAAAAAAAAAAOZNbQEAAAAAO%2BpmzNe9IamSzFMw32M%2F2y3sxUs%3DOtye0A4uJtk1BHs63mcs0qTAilhnZ6MnPFvx7aa16PazaR0PQN"
  val twitterHeaderBearerTokenHeader: Header.Raw = Header.Raw(CIString("Authorization"), s"Bearer $twitterBearerToken")
  val twitterHeaders: Headers = Headers(twitterHeaderBearerTokenHeader)

  val ethplorerHost = "https://api.ethplorer.io"
  val ethplorerApiKey = "freekey"
  val ethplorerImageHost = "https://ethplorer.io"

  val alchemyHost = "https://eth-mainnet.alchemyapi.io"
  val alchemyKey = "XLf70gRxS2FpIWTNDGm2-wcrdP5yzgOS"

  val openseaApiKey = "f4104ad1cfc544cdaa7d4e1fb1273fc8"
  val openseaHost = "https://api.opensea.io"
  val openseaHeaders: Headers = Headers(
    Header.Raw(CIString("X-Api-Key"), openseaApiKey),
    Header.Raw(CIString("Accept"), "application/json"),
  )

  val web3ConnectionString = "https://speedy-nodes-nyc.moralis.io/78759d66ae649f7b3ea47aaa/eth/mainnet"
  private lazy val web3Connection = new HttpService(web3ConnectionString)
  private lazy val web3j = Web3j.build(web3Connection)
  private lazy val ensResolver = new EnsResolver(web3j)

  val big10e18: BigDecimal = BigDecimal("1000000000000000000")

  def toEth(wei: String): Double = (BigDecimal(wei) / big10e18).toDouble

  def toEth(wei: BigInt): Double = (BigDecimal(wei) / big10e18).toDouble

  def toEth(wei: BigDecimal): Double = (wei / big10e18).toDouble


  def addressType(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressTypeResponse]] = {
    val path = "api"
    val queryParams = Map(
      "module" -> "contract",
      "action" -> "getabi",
      "address" -> address,
      "apikey" -> etherScanApiKey,
    )

    SimpleHttpClient
      .call[EtherscanContract, AddressTypeResponse](
        host = etherScanHost,
        path = path,
        queryParams = queryParams,
        conversion = c =>
          AddressTypeResponse(addressType = if (c.status == "1") AddressType.contract else AddressType.external)
      )
  }

  def addressHistoryEtherscan(
    receivingAddress: String,
    sendingAddress: Option[String],
    limit: Int = 100,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressHistoryResponse]] = {
    val path = s"api"
    val startBlock = "0"
    val endblock = "99999999"
    val page = "1"
    val offset = s"$limit"
    val sort = "desc"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "txlist",
      "address" -> receivingAddress,
      "startblock" -> startBlock,
      "endblock" -> endblock,
      "page" -> page,
      "offset" -> offset,
      "sort" -> sort,
      "apikey" -> etherScanApiKey,
    )

    SimpleHttpClient
      .call[EtherscanTransactions, AddressHistoryResponse](
        host = etherScanHost,
        path = path,
        queryParams = queryParams,
        conversion = data => {
          val filteredData = sendingAddress match {
            case Some(address) => EtherscanTransactions(
              status = data.status,
              message = data.message,
              result = data
                .result
                .filter(r => r.from.toLowerCase == address.toLowerCase || r.to.toLowerCase == address.toLowerCase)
              ,
            )
            case None => data
          }
          AddressHistoryResponse(filteredData)
        }
      )
  }

  def addressTokenHistoryEtherscan(
    address: String,
    limit: Int = 100,
    startBlock: String = "0",
    endBlock: String = "99999999",
    sort: String = "desc"
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressHistoryResponse]] = {
    val path = s"api"
    val page = "1"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "tokentx",
      "address" -> address,
      "startblock" -> startBlock,
      "endblock" -> endBlock,
      "page" -> page,
      "offset" -> s"$limit",
      "sort" -> sort,
      "apikey" -> etherScanApiKey,
    )
    SimpleHttpClient
      .call[EtherscanTokenTransactions, AddressHistoryResponse](
        host = etherScanHost,
        path = path,
        queryParams = queryParams,
        conversion = tokenTransactions => AddressHistoryResponse(tokenTransactions)
      )
  }

  def combinedAddressHistory(
    receivingAddress: String,
    sendingAddress: Option[String],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressHistoryResponse]] = {
    //    val instant = ZonedDateTime.now().minusMonths(3).toInstant
    val chain = for {
      //      startBlockNumber <- EitherT(getBlockFromDate(instant.toEpochMilli.toString))
      etherscanTransactions <- EitherT(addressHistoryEtherscan(receivingAddress, sendingAddress))
      etherscanTokenTransactions <- EitherT(addressTokenHistoryEtherscan(receivingAddress)) //, startBlock = startBlockNumber.block.toString))
    } yield
      AddressHistoryResponse(
        entries = (etherscanTransactions.entries ++ etherscanTokenTransactions.entries).sortBy(-_.timestamp.toInt)
      )

    chain.value
  }

  def addressTokensEthplorer(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, EthplorerTokens]] = {
    val path = s"getAddressInfo/$address"
    val queryParams = Map(
      "apiKey" -> ethplorerApiKey,
    )
    SimpleHttpClient
      .call[EthplorerTokens, EthplorerTokens](
        host = ethplorerHost,
        path = path,
        queryParams = queryParams,
        conversion = t => t
      )
  }

  //  def addressNfts(
  //    address: String,
  //  )(implicit
  //    client: Client[IO],
  //  ): IO[Either[ErrorResponseTrait, NftsResponse]] = {
  //    val path = s"api/v2/$address/nft"
  //    val queryParams = Map(
  //      "chain" -> "eth",
  //    )
  //
  //    SimpleHttpClient
  //      .call[MoralisNfts, NftsResponse](
  //        host = moralisHost,
  //        path = path,
  //        queryParams = queryParams,
  //        conversion = data => NftsResponse(nfts = data.result.map(Nft(_)).toList),
  //        headers = moralisApiKeyHeaderInHeader,
  //      )
  //  }

  def addressNfts(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, NftsResponse]] = {
    val path = s"v2/$alchemyKey/getNFTs/"
    val queryParams = Map(
      "owner" -> address,
    )

    val headers = Headers(
      Header.Raw(CIString("Content-Type"), "application/json"),
      Header.Raw(CIString("Accept"), "*/*"),
      //      Header.Raw(CIString("Accept-Encoding"), "gzip, deflate, br")
    )

    //    SimpleHttpClient
    //      .call[AlchemyNftsResponse, NftsResponse](
    //        host = alchemyHost,
    //        path = path,
    //        queryParams = queryParams,
    //        conversion = data => {
    //          NftsResponse(nfts =
    //            data.ownedNfts.map(nft =>
    //              Nft(
    //                tokenAddress = nft.contract.flatMap(_.address),
    //                contractType = None,
    //                collectionName = nft.title,
    //                symbol = None,
    //                name = nft.title,
    //                imageUrl =  None, //nft.metadata.flatMap(_.imageUrl),
    //                description = nft.description,
    //              )
    //            )
    //          )
    //        },
    //        headers = headers,
    //      )
    SimpleHttpClient
      .call[Json, NftsResponse](
        host = alchemyHost,
        path = path,
        queryParams = queryParams,
        conversion = json => {
          val nfts = root.ownedNfts.arr.getOption(json).getOrElse(Vector.empty).toList
          NftsResponse(nfts =
            nfts.map { nftJson =>
              import cats.implicits._
              val contract = root.contract.address.string.getOption(nftJson)
              val image1 = root.metadata.image.string.getOption(nftJson)
              val image2 = root.metadata.imageUrl.string.getOption(nftJson)
              val nft =
                Nft(
                  tokenAddress = root.contract.address.string.getOption(nftJson),
                  contractType = root.id.tokenMetadata.tokenType.string.getOption(nftJson),
                  collectionName = root.title.string.getOption(nftJson),
                  symbol = None,
                  name = root.metadata.name.string.getOption(nftJson),
                  imageUrl = image1 <+> image2,
                  description = root.metadata.description.string.getOption(nftJson),
                )
              nft
            }
          )
        },
        headers = headers,
      )
  }

  def addressVolume(
    receivingAddress: String,
    sendingAddress: Option[String],
    limit: Int = 5000,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressVolumeResponse]] = {
    val path = s"api"
    val startBlock = "0"
    val endblock = "99999999"
    val page = "1"
    val offset = s"$limit"
    val sort = "desc"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "txlist",
      "address" -> receivingAddress,
      "startblock" -> startBlock,
      "endblock" -> endblock,
      "page" -> page,
      "offset" -> offset,
      "sort" -> sort,
      "apikey" -> etherScanApiKey,
    )

    val volumeCall = EitherT(
      SimpleHttpClient
        .call[EtherscanTransactions, AddressVolumeResponse](
          host = etherScanHost,
          path = path,
          queryParams = queryParams,
          conversion = data => {
            val filteredData = sendingAddress match {
              case Some(address) => EtherscanTransactions(
                status = data.status,
                message = data.message,
                result = data.result.filter(r => r.from.toLowerCase == address.toLowerCase || r.to.toLowerCase == address.toLowerCase),
              )
              case None => data
            }
            AddressVolumeResponse(filteredData, receivingAddress)
          }
        ))

    val conversionCall: EitherT[IO, ErrorResponseTrait, ConversionResult] = EitherT(convertCurrency("ethereum", "usd"))

    val chain =
      for {
        volume <- volumeCall
        conversion <- conversionCall
      } yield
        volume
          .copy(volumeInUSD = volume.volumeInWei.map(v => conversion.conversion.toDouble * toEth(v)))
          .copy(volumeOutUSD = volume.volumeOutWei.map(v => conversion.conversion.toDouble * toEth(v)))

    chain.value

  }

  def convertCurrency(
    from: String,
    to: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, ConversionResult]] = {
    // https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=USD

    val fromClean = from.toLowerCase
    val toClean = to.toLowerCase
    val path = "api/v3/simple/price"
    val queryParams = Map(
      "ids" -> fromClean,
      "vs_currencies" -> toClean
    )
    val call: IO[Either[ErrorResponseTrait, Either[Throwable, ConversionResult]]] =
      SimpleHttpClient
        .call[Json, Either[Throwable, ConversionResult]](
          host = coinGeckoHost,
          path = path,
          queryParams = queryParams,
          conversion = data => {
            val temp: Either[IllegalStateException, ConversionResult] = {
              root.selectDynamic(fromClean).usd.double
                .getOption(data)
                .toRight(new IllegalStateException(s"Could not extract data from $fromClean to USD conversion"))
                .map(res => ConversionResult(
                  conversion = res.toString,
                  conversionUnit = "USD",
                ))
            }
            temp
          }
        )

    val maps: IO[Either[ErrorResponseTrait, ConversionResult]] =
      call.flatMap(e => IO(
        e match {
          case Left(err) => Left(err)
          case Right(v) => v match {
            case Left(err2) => Left(ErrorResponse(err2.getMessage))
            case Right(v) => Right(v)
          }
        }
      ))
    maps
  }

  def addressBalance(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressBalanceResponse]] = {
    val path = "api"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "balance",
      "address" -> address,
      "apikey" -> etherScanApiKey,
    )

    val balanceCall: EitherT[IO, ErrorResponseTrait, AddressBalanceResponse] = EitherT(
      SimpleHttpClient
        .call[EtherscanBalance, AddressBalanceResponse](
          host = etherScanHost,
          path = path,
          queryParams = queryParams,
          conversion = c =>
            AddressBalanceResponse(
              balance = Some(toEth(BigDecimal(c.result)).toString),
              symbol = Some("ETH"),
            )
        ))

    val conversionCall: EitherT[IO, ErrorResponseTrait, ConversionResult] = EitherT(convertCurrency("ethereum", "usd"))

    val chain =
      for {
        balance <- balanceCall
        conversion <- conversionCall
      } yield
        balance.copy(balanceUSD = balance.balance.map(b => conversion.conversion.toDouble * b.toDouble))

    chain.value
  }

  def getBlockFromDate(
    date: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, MoralisDateBlockResponse]] = {
    val path = s"api/v2/dateToBlock"
    val queryParams = Map(
      "chain" -> "eth",
      "date" -> date,
    )

    SimpleHttpClient
      .call[MoralisDateBlockResponse, MoralisDateBlockResponse](
        host = moralisHost,
        path = path,
        queryParams = queryParams,
        conversion = a => a,
        headers = moralisApiKeyHeaderInHeader,
      )
  }

  def tokenBalance(
    addressHistoryEntry: AddressHistoryEntry,
    contractAddress: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressBalanceResponse]] = {
    val path = "api"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "tokenbalance",
      "contractaddress" -> contractAddress,
      "address" -> addressHistoryEntry.to,
      "apikey" -> etherScanApiKey,
    )

    val balanceCall: EitherT[IO, ErrorResponseTrait, EtherscanBalance] = EitherT(
      SimpleHttpClient
        .call[EtherscanBalance, EtherscanBalance](
          host = etherScanHost,
          path = path,
          queryParams = queryParams,
          conversion = b => b
        ))

    val conversionCall = addressHistoryEntry
      .tokenSymbol
      .map(s => convertCurrency(s, "usd")
        .flatMap {
          case Right(value) => IO(Some(value))
          case Left(_) => IO(None)
        }
      ).getOrElse(IO(None))

    val chain =
      for {
        balance <- balanceCall
        conversion <- EitherT(conversionCall.map(Right(_).asInstanceOf[Either[ErrorResponseTrait, Option[ConversionResult]]]))
      } yield {
        val balanceUSD =
          for {
            balanceResult <- Option(balance.result).filter(s => s != null && s.nonEmpty)
            balance <- Try(balanceResult.toDouble).toOption
            conversionFactor <- conversion.flatMap(c => Try(c.conversion.toDouble).toOption)
            decimals <- addressHistoryEntry.tokenDecimal.flatMap(s => Try(s.toDouble).toOption)
            power = math.pow(10, decimals)
          } yield (balance / power) * conversionFactor
        AddressBalanceResponse(
          balance = Option(balance.result),
          symbol = addressHistoryEntry.tokenSymbol,
          balanceUSD = balanceUSD,
        )
      }

    chain.value
  }

  def toNativeValue(
    ethplorerToken: EthplorerToken,
  ): Option[Double] = {
    for {
      balanceResult <- ethplorerToken.rawBalance.filter(s => s != null && s.nonEmpty)
      balance <- Try(balanceResult.toDouble).toOption
      decimals <- ethplorerToken.tokenInfo.decimals.flatMap(s => Try(s.toDouble).toOption)
      power = math.pow(10, decimals)
    } yield balance / power
  }

  def toNativeValueConverted(
    balance: Double,
    conversionResult: ConversionResult,
  ): Option[Double] =
    Try(conversionResult.conversion.toDouble)
      .toOption
      .map(_ * balance)

  def addressTokens(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, AddressTokensResponse]] = {

    val chain = for {
      tokens <- EitherT(addressTokensEthplorer(address))
        .map(ethplorerTokens => ethplorerTokens
          .tokens
          .map(_.map {
            token =>
              AddressToken(
                contractAddress = token.tokenInfo.address,
                value = toNativeValue(token),
                rawValue = token.rawBalance,
                valueUSD = None,
                tokenName = token.tokenInfo.name,
                tokenSymbol = token.tokenInfo.symbol,
                tokenDecimal = token.tokenInfo.decimals,
                imageUrl = token.tokenInfo.image.map(i => s"$ethplorerImageHost$i"),
              )
          })
        )
    } yield AddressTokensResponse(tokens)

    chain.value
  }

  def lists(listRequest: String): IO[Either[ErrorResponseTrait, ListResponse]] = {
    val data = listRequest match {
      case "tilli" => TilliList.tilliList.map(l => ListResponse(l)).leftMap(_ => ErrorResponse("An error occurred while loading a list"))
      case _ => EitherT(IO(Left(ErrorResponse(s"Unknown list $listRequest")).asInstanceOf[Either[ErrorResponse, ListResponse]]))
    }
    data.value
  }


  def ensResolution(address: String): IO[Either[ErrorResponseTrait, EnsResolutionResponse]] = {

    def call(address: String, f: String => String): Either[Throwable, Option[String]] =
      Try(f(address))
        .toEither
        .map(Option(_).filter(s => s != null || s.nonEmpty))

    def cleanAddress(address: String): Either[Throwable, String] = {
      Option(address).filter(s => s != null && s.nonEmpty) // TODO: Add check that the address is valid
        .toRight(new IllegalArgumentException("Invalid address input"))
    }

    val chain = for {
      cleanedAddress <- EitherT(IO(cleanAddress(address)))
      resolved <- EitherT(IO(Right(call(cleanedAddress, ensResolver.resolve).toOption.flatten).asInstanceOf[Either[Throwable, Option[String]]]))
      reverseResolved <- EitherT(IO(Right(call(cleanedAddress, ensResolver.reverseResolve).toOption.flatten).asInstanceOf[Either[Throwable, Option[String]]]))
    } yield EnsResolutionResponse(
      address = resolved,
      reverseAddress = reverseResolved,
    )

    chain
      .leftMap(e => ErrorResponse(s"An error occurred during ENS resolution: ${
        e.getMessage
      }"))
      .value
  }

  // https://api.twitter.com/1.1/users/search.json?q=adschwartz.eth
  def twitterHandle(
    ens: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, TwitterResponses]] = {

    def toTwitterResponse(responsesRaw: List[TwitterResponseRaw]): List[TwitterResponse] =
      responsesRaw.map(tr =>
        TwitterResponse(
          name = tr.name,
          handle = tr.screenName,
          description = tr.description,
          url = tr.screenName.map(s => s"https://twitter.com/$s"),
          imageUrl = tr.profileImageUrlHttps,
          followersCount = tr.followersCount,
          friendsCount = tr.friendsCount,
        )
      ).sortBy(_.followersCount).reverse

    val path = s"1.1/users/search.json"
    val queryParams = Map(
      "q" -> ens,
    )
    SimpleHttpClient
      .call[List[TwitterResponseRaw], TwitterResponses](
        host = twitterHost,
        path = path,
        queryParams = queryParams,
        conversion = trs => {
          val trimmedEns = ens.toLowerCase.trim
          val exactMatch = trs.filter(_.name.exists(_.toLowerCase.trim == trimmedEns))
          val responses = if (exactMatch.nonEmpty) toTwitterResponse(exactMatch)
          else toTwitterResponse(trs)
          TwitterResponses(responses)
        },
        headers = twitterHeaders,
      )
  }

  //  def getNftAnalytics(
  //    collectionSlug: String,
  //    uuid: UUID,
  //  )(implicit
  //    client: Client[IO],
  //  ): IO[Either[ErrorResponseTrait, Unit]] = {
  //    println(s"Starting $uuid! ($collectionSlug $uuid ${getTimestamp()})")
  //    //    val distinctNftSlugs = List(NftMarketData(collectionOpenSeaSlug = Some("flyfrogs-ii8t6qt5t5")))
  //    //    val ownerAssets = List(NftAssetOwner(None, Some("0x79FCDEF22feeD20eDDacbB2587640e45491b757f"), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None))
  //    //    val distinctNftSlugs = List(NftMarketData(collectionOpenSeaSlug = Some("floor-app"), assetContractAddress = Some("0x79FCDEF22feeD20eDDacbB2587640e45491b757f")))
  //    val chain =
  //    for {
  //      ownerAssets <- EitherT(getOwnerAssets(collectionSlug, uuid))
  //      distinctNftSlugs <- EitherT(IO(Right(getDistinctNftCollections(ownerAssets)).asInstanceOf[Either[ErrorResponseTrait, List[NftMarketData]]]))
  //      marketData <- EitherT(getMarketData(distinctNftSlugs, uuid))
  //      enrichedOwnerAssets = enrichNftAssetsWithMarketData(ownerAssets, marketData)
  //        .filter(_.floorPrice.exists(_ >= 0.05))
  //      write <- EitherT(writeToFile(enrichedOwnerAssets, uuid, collectionSlug)).leftMap(e => ErrorResponse(e.getMessage).asInstanceOf[ErrorResponseTrait])
  //      _ = println(s"Done $uuid! ($uuid ${getTimestamp()})")
  //    } yield write
  //    chain
  //      .leftSemiflatTap(err => IO(s"Error: ${println(err.message)}"))
  //      .value
  //  }

  def enrichNftAssetsWithMarketData(
    nftAssets: List[NftAsset],
    marketData: List[NftMarketData],
  ): List[NftAsset] = {
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
      .distinctBy(_.assetContractAddress.map(_.toLowerCase()))

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

  private val PATTERN_FORMAT = "yyyy-MM-dd"
  private val formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

  def getTimestamp(now: Instant = Instant.now()): String = now.toString

  //  def getOwnerAssets(
  //    collectionSlug: String,
  //    uuid: UUID,
  //  )(implicit
  //    client: Client[IO],
  //  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
  //    val chain =
  //      for {
  //        owners <- EitherT(getOwnersOfNftCollection(collectionSlug, uuid))
  //        _ = println(s"Got owners ($uuid ${getTimestamp()}) ${owners}")
  //        assets <- EitherT(getOwnerNFTAssets(owners, uuid))
  //      } yield assets
  //    chain.value
  //  }

  def getMarketData(
    nftCollections: List[NftMarketData],
    uuid: UUID,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftMarketData]]] = {
    import cats.implicits._
    val total = nftCollections.size
    var counter = 1
    fs2.Stream
      .iterable(nftCollections)
      .evalMap(nftCollection =>
        IO(println(s"getMarketData(${nftCollection.collectionOpenSeaSlug}) ($uuid ${getTimestamp()}) $counter/$total")) *>
          IO {
            counter = counter + 1
          } *> getMarketData(nftCollection) <* Temporal[IO].sleep(300.milliseconds)
      )
      .takeWhile(_.isRight)
      .compile
      .toList
      .map(_.sequence)
  }

  def getMarketData(
    nftMarketData: NftMarketData,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, NftMarketData]] = {
    // https://api.opensea.io/api/v1/collection/philosophicalfoxes/stats
    val path = s"api/v1/collection/${nftMarketData.collectionOpenSeaSlug.get}/stats"
    val queryParams = Map.empty[String, String]

    SimpleHttpClient
      .call[Json, Json](
        host = openseaHost,
        path = path,
        queryParams = queryParams,
        conversion = json => json,
        headers = openseaHeaders,
      )
      .map {
        case Left(err) => err.code match {
          case Some("404") => Right(nftMarketData)
          case Some("500") => Right(nftMarketData)
          case _ => Left(err)
        }
        case Right(json) =>
          import io.circe.optics.JsonPath.root
          Right(
            nftMarketData.copy(
              numberOfOwners = root.stats.numOwners.int.getOption(json),
              floorPrice = root.stats.floorPrice.double.getOption(json),
              averagePrice = root.stats.averagePrice.double.getOption(json),
              marketCap = root.stats.marketCap.double.getOption(json),
              totalVolume = root.stats.totalVolume.double.getOption(json),
            )
          )
      }
  }

  def getOwnersOfNftCollection(
    collectionSlug: String,
    uuid: UUID,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
    import cats.implicits._
    // https://api.opensea.io/api/v1/assets?collection_slug=philosophicalfoxes&limit=200
    val path = "api/v1/assets"
    val queryParams = Map(
      "collection_slug" -> collectionSlug,
      "limit" -> "200",
      "order_direction" -> "desc",
      "include_orders" -> "false",
    )
    SimpleHttpClient
      .callPaged[Json, Json](
        host = openseaHost,
        path = path,
        pageParamKey = "next",
        cursorQueryParamKey = "cursor",
        queryParams = queryParams,
        conversion = json => json,
        headers = openseaHeaders,
        uuid = Some(uuid),
      )
      .map(_.sequence)
      .map {
        case Left(err) => Left(err)
        case Right(jsonList) =>
          import io.circe.optics.JsonPath.root
          val allAddresses = jsonList.foldLeft(List.empty[NftAsset]) { (acc, json) =>
            acc ++
              root.assets
                .arr
                .getOption(json)
                .toList
                .flatMap(_.map(e =>
                  NftAsset(
                    ownerAddress = root.owner.address.string.getOption(e),
                    assetContractType = root.assetContract.assetContractType.string.getOption(e),
                  )
                ))
                .filter(nftAsset => nftAsset.ownerAddress != null && nftAsset.ownerAddress.nonEmpty)
          }.distinctBy(_.ownerAddress.map(_.toLowerCase))
          Right(allAddresses)
      }
  }

  def getNftCollectionSaleEvents(
    collectionSlug: String,
    uuid: UUID,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftSaleEvent]]] = {
    import cats.implicits._
    val path = "api/v1/events"
    val queryParams = Map(
      "collection_slug" -> collectionSlug,
      "limit" -> "200",
      "only_opensea" -> "false",
      "event_type" -> "transfer",
    )
    SimpleHttpClient
      .callPaged[Json, Json](
        host = openseaHost,
        path = path,
        pageParamKey = "next",
        cursorQueryParamKey = "cursor",
        queryParams = queryParams,
        conversion = json => json,
        headers = openseaHeaders,
        uuid = Some(uuid),
        sleepMs = 50,
      )
      .map(_.sequence)
      .map {
        case Left(err) => Left(err)
        case Right(jsonList) =>
          import io.circe.optics.JsonPath.root
          val allAddresses = jsonList.foldLeft(List.empty[NftSaleEvent]) { (acc, json) =>
            acc ++
              root
                .assetEvents
                .arr
                .getOption(json)
                .toList
                .flatMap(_.map(e =>
                  NftSaleEvent(
                    transactionHash = root.transaction.transactionHash.string.getOption(e),
                    eventType = root.eventType.string.getOption(e),
                    fromAddress = root.fromAccount.address.string.getOption(e), // fromAccount when transfer
                    toAddress = root.toAccount.address.string.getOption(e), // toAccount when transfer
//                    fromAddress = root.seller.address.string.getOption(e), // seller when successful
//                    toAddress = root.winnerAccount.address.string.getOption(e), // winnerAccount when successful
                    tokenId = root.asset.tokenId.string.getOption(e),
                    quantity = root.quantity.string.getOption(e).flatMap(s => Try(s.toInt).toOption),
                    timestamp = root.eventTimestamp.string.getOption(e)
                      .map(ts => if(!ts.toLowerCase.endsWith("z")) s"${ts}Z" else ts)
                      .flatMap(ts => Try(Instant.parse(ts)).toOption),
                  )
                ))
//                .filter(nftAsset => nftAsset.toAddress.exists(_!= null) && nftAsset.toAddress.nonEmpty)
          }
          Right(allAddresses)
      }
  }

//  def getOwnerNFTAssets(
//    owners: List[String],
//    uuid: UUID,
//    sleepMs: DurationInt = 250,
//  )(implicit
//    client: Client[IO],
//  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
//    import cats.implicits._
//    val total = owners.size
//    var counter = 1
//    fs2.Stream
//      .iterable(owners)
//      .evalMap(owner => (IO(println(s"getOwnerNFTAssets($owner) ($uuid ${getTimestamp()}) $counter/$total")) *>
//        IO {
//          counter = counter + 1
//        } *> getOwnerNFTAssets(owner, uuid,sleepMs)))
//      .takeWhile(_.isRight)
//      .compile
//      .toList
//      .map(_.sequence)
//      .map(_.map(_.flatten))
//  }

  def getOwnerNFTAssets(
    owner: String,
    uuid: UUID,
    sleepMs: DurationInt = 250
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponseTrait, List[NftAsset]]] = {
    // https://api.opensea.io/api/v1/assets?collection_slug=philosophicalfoxes&limit=200
    val path = "api/v1/assets"
    val queryParams = Map(
      "owner" -> owner,
      "limit" -> "200",
      "order_direction" -> "desc",
      "include_orders" -> "false",
    )

    import cats.implicits._

    SimpleHttpClient
      .callPaged[Json, Json](
        host = openseaHost,
        path = path,
        pageParamKey = "next",
        cursorQueryParamKey = "cursor",
        queryParams = queryParams,
        conversion = json => json,
        headers = openseaHeaders,
        uuid = Some(uuid),
        sleepMs,
      )
      .map(_.sequence)
      .map {
        case Left(err) => Left(err)
        case Right(jsonList) =>

          import io.circe.optics.JsonPath.root

          val allAssets = jsonList.foldLeft(List.empty[NftAsset]) {
            (acc, json) =>
              acc ++
                root.assets
                  .arr
                  .getOption(json)
                  .toList
                  .flatMap(_.map(json => extractNftAssetOwnerDetails(json, owner)))
          }
          Right(allAssets)
      }
  }

  def extractNftAssetOwnerDetails(json: Json, owner: String): NftAsset = {

    import io.circe.optics.JsonPath.root

    NftAsset(
      ownerAddress = Option(owner),

      assetContractAddress = root.assetContract.address.string.getOption(json),
      assetContractType = root.assetContract.assetContractType.string.getOption(json),
      assetContractName = root.assetContract.name.string.getOption(json),
      assetContractUrl = root.assetContract.externalLink.string.getOption(json),
      assetContractSchema = root.assetContract.schemaName.string.getOption(json),
      tokenId = root.tokenId.string.getOption(json),
      collectionName = root.collection.name.string.getOption(json),
      collectionOpenSeaSlug = root.collection.slug.string.getOption(json),
      collectionUrl = root.collection.externalUrl.string.getOption(json),
      collectionDiscord = root.collection.discordUrl.string.getOption(json),
      collectionTelegram = root.collection.telegramUrl.string.getOption(json),
      collectionTwitterUsername = root.collection.twitterUsername.string.getOption(json),
      collectionInstagram = root.collection.instagramUsername.string.getOption(json),
      collectionWiki = root.collection.wikiUrl.string.getOption(json),
      collectionMediumUsername = root.collection.mediumUsername.string.getOption(json),

      count = None,
      numberOfOwners = None,
      floorPrice = None,
      averagePrice = None,
      marketCap = None,
      totalVolume = None,
    )

  }

}
