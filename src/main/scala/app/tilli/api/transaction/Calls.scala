package app.tilli.api.transaction

import app.tilli.api.list.TilliList
import app.tilli.api.utils.SimpleHttpClient
import app.tilli.codec.AddressType
import app.tilli.codec.TilliClasses._
import app.tilli.codec.TilliCodecs._
import cats.data.EitherT
import cats.effect.IO
import io.circe.Json
import io.circe.optics.JsonPath._
import org.http4s.client.Client
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIString

import scala.util.Try

object Calls {

  val etherScanHost = "https://api.etherscan.io"
  val etherScanApiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"
  val moralisHost = "https://deep-index.moralis.io"
  val moralisApiKey = "gyk7fYMB0EOekZxvsLEDyE0Pm46H6py7iwn0x0fr7ortbcMPmUef0GPnHHtc8upP"
  val coinGeckoHost = "https://api.coingecko.com"

  val ethplorerHost = "https://api.ethplorer.io"
  val ethplorerApiKey = "freekey"
  val ethplorerImageHost = "https://ethplorer.io"

  val big10e18: BigDecimal = BigDecimal("1000000000000000000")

  def toEth(wei: String): Double = (BigDecimal(wei) / big10e18).toDouble

  def toEth(wei: BigInt): Double = (BigDecimal(wei) / big10e18).toDouble

  def toEth(wei: BigDecimal): Double = (wei / big10e18).toDouble

  val moralisApiKeyHeader: Header.Raw = Header.Raw(CIString("X-Api-Key"), moralisApiKey)

  val moralisApiKeyHeaderInHeader: Headers = Headers(moralisApiKeyHeader)

  def addressType(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, AddressTypeResponse]] = {
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
  ): IO[Either[ErrorResponse, AddressHistoryResponse]] = {
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
  ): IO[Either[ErrorResponse, AddressHistoryResponse]] = {
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
  ): IO[Either[ErrorResponse, AddressHistoryResponse]] = {
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
  ): IO[Either[ErrorResponse, EthplorerTokens]] = {
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

  def addressNfts(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, NftsResponse]] = {
    val path = s"api/v2/$address/nft"
    val queryParams = Map(
      "chain" -> "eth",
    )

    SimpleHttpClient
      .call[MoralisNfts, NftsResponse](
        host = moralisHost,
        path = path,
        queryParams = queryParams,
        conversion = data => NftsResponse(nfts = data.result.map(Nft(_)).toList),
        headers = moralisApiKeyHeaderInHeader,
      )
  }

  def addressVolume(
    receivingAddress: String,
    sendingAddress: Option[String],
    limit: Int = 5000,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, AddressVolumeResponse]] = {
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

    val conversionCall: EitherT[IO, ErrorResponse, ConversionResult] = EitherT(convertCurrency("ethereum", "usd"))

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
  ): IO[Either[ErrorResponse, ConversionResult]] = {
    // https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=USD

    val fromClean = from.toLowerCase
    val toClean = to.toLowerCase
    val path = "api/v3/simple/price"
    val queryParams = Map(
      "ids" -> fromClean,
      "vs_currencies" -> toClean
    )
    val call: IO[Either[ErrorResponse, Either[Throwable, ConversionResult]]] =
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

    val maps: IO[Either[ErrorResponse, ConversionResult]] =
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
  ): IO[Either[ErrorResponse, AddressBalanceResponse]] = {
    val path = "api"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "balance",
      "address" -> address,
      "apikey" -> etherScanApiKey,
    )

    val balanceCall: EitherT[IO, ErrorResponse, AddressBalanceResponse] = EitherT(
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

    val conversionCall: EitherT[IO, ErrorResponse, ConversionResult] = EitherT(convertCurrency("ethereum", "usd"))

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
  ): IO[Either[ErrorResponse, MoralisDateBlockResponse]] = {
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
  ): IO[Either[ErrorResponse, AddressBalanceResponse]] = {
    val path = "api"
    val queryParams = Map(
      "module" -> "account",
      "action" -> "tokenbalance",
      "contractaddress" -> contractAddress,
      "address" -> addressHistoryEntry.to,
      "apikey" -> etherScanApiKey,
    )

    val balanceCall: EitherT[IO, ErrorResponse, EtherscanBalance] = EitherT(
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
        conversion <- EitherT(conversionCall.map(Right(_).asInstanceOf[Either[ErrorResponse, Option[ConversionResult]]]))
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
  ): IO[Either[ErrorResponse, AddressTokensResponse]] = {

    val chain = for {
      tokens <- EitherT(addressTokensEthplorer(address))
        .map(ethplorerTokens => ethplorerTokens
          .tokens
          .map(_.map { token =>
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

  def lists(listRequest: String): IO[Either[ErrorResponse, ListResponse]] = {
    val data = listRequest match {
      case "tilli" => TilliList.tilliList.map(l => ListResponse(l)).leftMap(e => ErrorResponse("An error occurred while loading a list"))
      case _ => EitherT(IO(Left(ErrorResponse(s"Unknown list $listRequest")).asInstanceOf[Either[ErrorResponse, ListResponse]]))
    }
    data.value
  }

}
