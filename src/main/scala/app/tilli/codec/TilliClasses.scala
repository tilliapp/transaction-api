package app.tilli.codec

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.{Base64, UUID}
import scala.util.Try

object TilliClasses {

  // External
  case class EtherscanContract(
    status: String,
    message: String,
    result: String,
  )

  case class EtherscanTransaction(
    blockNumber: String,
    timeStamp: String,
    hash: String,
    nonce: String,
    blockHash: String,
    transactionIndex: String,
    from: String,
    to: String,
    value: String,
    gas: String,
    gasPrice: String,
    isError: String,
    txreceiptStatus: String,
    input: String,
    contractAddress: String,
    cumulativeGasUsed: String,
    gasUsed: String,
    confirmations: String,
  )

  case class EtherscanTokenTransaction(
    blockNumber: String,
    timeStamp: String,
    hash: String,
    nonce: String,
    blockHash: String,
    transactionIndex: String,
    from: String,
    to: String,
    value: String,
    gas: String,
    gasPrice: String,
    contractAddress: String,
    cumulativeGasUsed: String,
    gasUsed: String,
    confirmations: String,
    tokenSymbol: String,
    tokenName: String,
    tokenDecimal: String,
  )

  case class EtherscanTransactions(
    status: String,
    message: String,
    result: List[EtherscanTransaction]
  )

  case class EtherscanTokenTransactions(
    status: String,
    message: String,
    result: List[EtherscanTokenTransaction]
  )

  case class EtherscanBalance(
    status: String,
    message: String,
    result: String,
  )

  case class MoralisNftMetadata(
    name: String,
    description: String,
    image: String,
  )

  case class MoralisNft(
    tokenAddress: String,
    //    tokenId: String,
    amount: Int,
    contractType: Option[String],
    name: Option[String],
    symbol: Option[String],
    tokenUri: Option[String],
    //    metadata: Json,
  )

  case class MoralisNfts(
    //    total: Int,
    //    page: Int,
    //    pageSize: Int,
    result: Array[MoralisNft],
  )

  case class MoralisNftTokenUri(
    name: String,
    description: String,
    image: String,
  )

  case class MoralisDateBlockResponse(
    date: String,
    block: Int,
    timestamp: Int, // Epoch in secs
  )

  // CoinGecko
  case class ConversionResult(
    conversion: String,
    conversionUnit: String,
  )

  // Ethplorer

  case class EthplorerPrice(
    rate: Option[Double],
    diff: Option[Double],
    diff7d: Option[Double],
    ts: Option[Int],
    marketCapUsd: Option[Double],
    availableSupply: Option[Double],
    volume24h: Option[Double],
    diff30d: Option[Double],
    volDiff1: Option[Double],
    volDiff7: Option[Double],
  )

  case class EthplorerEth(
    price: EthplorerPrice,
    balance: Option[Double],
    rawBalance: Option[String],
  )

  case class EthlorerTokenInfo(
    address: Option[String],
    decimals: Option[String],
    name: Option[String],
    owner: Option[String],
    symbol: Option[String],
    totalSupply: Option[String],
    lastUpdated: Option[Int],
    slot: Option[Int],
    issuancesCount: Option[Int],
    holdersCount: Option[Int],
    website: Option[String],
    twitter: Option[String],
    image: Option[String],
    coingecko: Option[String],
    ethTransfersCount: Option[Int],
    //    price: EthplorerPrice, // Some times is returned as "false" by ethplorer...
    //    publicTags: List[String],
  )

  case class EthplorerToken(
    tokenInfo: EthlorerTokenInfo,
    balance: Option[Double],
    totalIn: Option[Double],
    totalOut: Option[Double],
    rawBalance: Option[String],
  )

  case class EthplorerTokens(
    address: Option[String],
    ETH: EthplorerEth,
    countTxs: Option[Int],
    tokens: Option[List[EthplorerToken]],
  )

  trait ErrorResponseTrait {
    def message: String
  }

  // ****** Internal Responses
  case class ErrorResponse(
    message: String,
  ) extends ErrorResponseTrait

  case class AddressTypeResponse(
    addressType: AddressType.Value,
  )

  case class Nft(
    tokenAddress: Option[String],
    contractType: Option[String],
    collectionName: Option[String],
    symbol: Option[String],
    name: Option[String],
    imageUrl: Option[String],
    description: Option[String],
  )

  object Nft {

    import TilliCodecs._

    // data:application/json;base64,eyJuYW1lIjogIlRob3IiLCAiZGVzY3JpcHRpb24iOiAiIiwgImltYWdlIjoiaHR0cHM6Ly9jbG91ZGZsYXJlLWlwZnMuY29tL2lwZnMvUW1SN2NMcjRZZkZoSFNiQ25mM3RMdEs0WWJyVEFyQzZYN0cxdWJvWXJNVUY2YSJ9==
    def decodeTokenUri(tokenUri: String): Either[Throwable, MoralisNftTokenUri] =
      tokenUri.split(",")
        .lastOption
        .toRight(new IllegalStateException("No uri was extracted"))
        .flatMap { uriBase64 =>
          Try {
            val bytes = uriBase64.replaceAll("==", "").getBytes(StandardCharsets.UTF_8)
            val decoded = Base64.getDecoder.decode(bytes)
            val string = new String(decoded, StandardCharsets.UTF_8)
            string
          }.toEither
        }
        .flatMap(tokenUri => io.circe.parser.parse(tokenUri).flatMap(_.as[MoralisNftTokenUri]))

    def apply(moralisNft: MoralisNft): Nft = {
      val _tokenUriOpt = moralisNft.tokenUri.map(decodeTokenUri)
      if (_tokenUriOpt.exists(_.isLeft)) println(s"Error decoding token address ${moralisNft.tokenAddress}: ${_tokenUriOpt.get}")
      val tokenUri = _tokenUriOpt.flatMap(_.toOption)
      new Nft(
        tokenAddress = Option(moralisNft.tokenAddress),
        contractType = moralisNft.contractType,
        collectionName = moralisNft.name,
        symbol = moralisNft.symbol,
        name = tokenUri.map(_.name),
        imageUrl = tokenUri.map(_.image),
        description = tokenUri.map(_.description)
      )
    }
  }

  case class NftsResponse(
    nfts: List[Nft],
  )

  case class AddressHistoryEntry(
    transactionHash: String,
    timestamp: String,
    from: String,
    to: String,
    value: String,
    gas: String,
    gasPrice: String,
    //    input: String,
    tokenSymbol: Option[String],
    tokenName: Option[String],
    tokenDecimal: Option[String],
    contractAddress: Option[String],
  )

  object AddressHistoryEntry {

    def apply(etherscanTransaction: EtherscanTransaction): AddressHistoryEntry =
      AddressHistoryEntry(
        transactionHash = etherscanTransaction.hash,
        timestamp = etherscanTransaction.timeStamp,
        from = etherscanTransaction.from,
        to = etherscanTransaction.to,
        value = etherscanTransaction.value,
        gas = etherscanTransaction.gas,
        gasPrice = etherscanTransaction.gasPrice,
        //        input = etherscanTransaction.input,
        tokenSymbol = Some("ETH"),
        tokenName = Some("Ether"),
        tokenDecimal = Some("18"),
        contractAddress = Option(etherscanTransaction.contractAddress)
      )

    def apply(etherscanTransaction: EtherscanTokenTransaction): AddressHistoryEntry =
      AddressHistoryEntry(
        transactionHash = etherscanTransaction.hash,
        timestamp = etherscanTransaction.timeStamp,
        from = etherscanTransaction.from,
        to = etherscanTransaction.to,
        value = etherscanTransaction.value,
        gas = etherscanTransaction.gas,
        gasPrice = etherscanTransaction.gasPrice,
        //        input = etherscanTransaction.input,
        tokenSymbol = Option(etherscanTransaction.tokenSymbol),
        tokenName = Option(etherscanTransaction.tokenName),
        tokenDecimal = Option(etherscanTransaction.tokenDecimal),
        contractAddress = Option(etherscanTransaction.contractAddress)
      )

  }

  case class AddressHistoryResponse(
    entries: List[AddressHistoryEntry],
  )

  object AddressHistoryResponse {

    def apply(etherscanTransactions: EtherscanTransactions): AddressHistoryResponse =
      AddressHistoryResponse(
        entries = etherscanTransactions.result.map(AddressHistoryEntry(_))
      )

    def apply(etherscanTransactions: EtherscanTokenTransactions): AddressHistoryResponse =
      AddressHistoryResponse(
        entries = etherscanTransactions.result.map(AddressHistoryEntry(_))
      )
  }

  case class AddressVolumeResponse(
    transactionCountIn: Int,
    transactionCountOut: Int,
    volumeInWei: Option[String],
    volumeOutWei: Option[String],
    volumeInUSD: Option[Double] = None,
    volumeOutUSD: Option[Double] = None,
  )

  object AddressVolumeResponse {
    def apply(etherscanTransactions: EtherscanTransactions, address: String): AddressVolumeResponse = {

      def sum(txs: List[EtherscanTransaction]): BigInt = txs.foldLeft(BigInt(0)) { case (a, b) => a + BigInt(b.value) }

      val transactionsIn = etherscanTransactions.result.filter(_.to == address)
      val transactionsOut = etherscanTransactions.result.filter(_.from == address)

      val volumeIn = sum(transactionsIn).toString
      val volumeOut = sum(transactionsOut).toString

      AddressVolumeResponse(
        transactionCountIn = transactionsIn.length,
        transactionCountOut = transactionsOut.length,
        volumeInWei = Some(volumeIn),
        volumeOutWei = Some(volumeOut),
      )
    }
  }

  case class AddressBalanceResponse(
    balance: Option[String] = None,
    symbol: Option[String] = None,
    balanceUSD: Option[Double] = None,
  )

  case class AddressInformationResponse(
    `type`: AddressTypeResponse,
    history: AddressHistoryResponse,
    nfts: NftsResponse,
    volume: AddressVolumeResponse,
  )

  case class AddressToken(
    contractAddress: Option[String],
    value: Option[Double],
    rawValue: Option[String],
    valueUSD: Option[Double],
    tokenName: Option[String],
    tokenSymbol: Option[String],
    tokenDecimal: Option[String],
    imageUrl: Option[String],
  )

  case class AddressTokensResponse(
    tokens: Option[List[AddressToken]],
  )

  case class ListEntry(
    address: String,
    name: Option[String],
    description: Option[String],
    url: Option[String],
    date: Option[String],
    labels: Option[List[String]],
  )

  case class ListResponse(
    data: List[ListEntry],
  )

  case class EnsResolutionResponse(
    address: Option[String],
    reverseAddress: Option[String],
  )

  case class TwitterResponsesRaw(
    profiles: List[TwitterResponseRaw],
  )

  case class TwitterResponseRaw(
    name: Option[String],
    screenName: Option[String],
    description: Option[String],
    followersCount: Option[Int],
    friendsCount: Option[Int],
    profileImageUrlHttps: Option[String],
  )

  case class TwitterResponses(
    profiles: List[TwitterResponse],
  )

  case class TwitterResponse(
    name: Option[String],
    handle: Option[String],
    description: Option[String],
    url: Option[String],
    imageUrl: Option[String],
    followersCount: Option[Int],
    friendsCount: Option[Int],
  )

  case class NftAnalysisResponse(
    id: UUID,
  )

  case class NftAsset(
    ownerAddress: Option[String] = None,
    assetContractAddress: Option[String] = None,
    assetContractType: Option[String] = None,
    assetContractName: Option[String] = None,
    assetContractUrl: Option[String] = None,
    assetContractSchema: Option[String] = None,
    tokenId: Option[String] = None,
    collectionName: Option[String] = None,
    collectionOpenSeaSlug: Option[String] = None,
    collectionUrl: Option[String] = None,
    collectionDiscord: Option[String] = None,
    collectionTelegram: Option[String] = None,
    collectionTwitterUsername: Option[String] = None,
    collectionInstagram: Option[String] = None,
    collectionWiki: Option[String] = None,
    collectionMediumUsername: Option[String] = None,
    count: Option[Int] = None,
    numberOfOwners: Option[Int] = None,
    floorPrice: Option[Double] = None,
    averagePrice: Option[Double] = None,
    marketCap: Option[Double] = None,
    totalVolume: Option[Double] = None,
    createdAt: Option[Instant] = Some(Instant.now),
    updatedAt: Option[Instant] = Some(Instant.now),
  ) {

    def toStringList: List[String] =
      List(
        ownerAddress.getOrElse(null).asInstanceOf[String],
        assetContractAddress.getOrElse(null).asInstanceOf[String],
        assetContractType.getOrElse(null).asInstanceOf[String],
        //        assetContractName.getOrElse(null).asInstanceOf[String],
        //        assetContractUrl.getOrElse(null).asInstanceOf[String],
        assetContractSchema.getOrElse(null).asInstanceOf[String],
        tokenId.getOrElse(null).asInstanceOf[String],
        collectionName.getOrElse(null).asInstanceOf[String],
        collectionOpenSeaSlug.getOrElse(null).asInstanceOf[String],
        collectionUrl.getOrElse(null).asInstanceOf[String],
        //        collectionDiscord.getOrElse(null).asInstanceOf[String],
        //        collectionTelegram.getOrElse(null).asInstanceOf[String],
        //        collectionTwitterUsername.getOrElse(null).asInstanceOf[String],
        //        collectionInstagram.getOrElse(null).asInstanceOf[String],
        //        collectionWiki.getOrElse(null).asInstanceOf[String],
        //        collectionMediumUsername.getOrElse(null).asInstanceOf[String],
        count.map(_.toString).getOrElse(null).asInstanceOf[String],
        numberOfOwners.map(_.toString).getOrElse(null).asInstanceOf[String],
        floorPrice.map(_.toString).getOrElse(null).asInstanceOf[String],
        averagePrice.map(_.toString).getOrElse(null).asInstanceOf[String],
        marketCap.map(_.toString).getOrElse(null).asInstanceOf[String],
        totalVolume.map(_.toString).getOrElse(null).asInstanceOf[String],
      )
  }

  object NftAsset {
    def header: List[String] =
      List(
        "ownerAddress",
        "assetContractAddress",
        "assetContractType",
        //        "assetContractName",
        //        "assetContractUrl",
        "assetContractSchema",
        "tokenId",
        "collectionName",
        "collectionOpenSeaSlug",
        "collectionUrl",
        //        "collectionDiscord",
        //        "collectionTelegram",
        //        "collectionTwitterUsername",
        //        "collectionInstagram",
        //        "collectionWiki",
        //        "collectionMediumUsername",
        "count",
        "numberOfOwners",
        "floorPrice",
        "averagePrice",
        "marketCap",
        "totalVolume"
      )
  }

  case class NftSaleEvent(
    transactionHash: Option[String],
    eventType: Option[String],
    fromAddress: Option[String],
    toAddress: Option[String],
    tokenId: Option[String],
    quantity: Option[Int],
    timestamp: Option[Instant],
  )

  case class NftMarketData(
    assetContractAddress: Option[String] = None,
    collectionOpenSeaSlug: Option[String] = None,
    count: Option[Int] = None,
    numberOfOwners: Option[Int] = None,
    floorPrice: Option[Double] = None,
    averagePrice: Option[Double] = None,
    marketCap: Option[Double] = None,
    totalVolume: Option[Double] = None,
    createdAt: Instant = Instant.now,
    updatedAt: Instant = Instant.now,
  )

}