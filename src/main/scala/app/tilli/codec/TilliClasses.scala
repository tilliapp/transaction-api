package app.tilli.codec

import io.circe.Json

import java.nio.charset.StandardCharsets
import java.util.Base64
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

  case class EtherscanTransactions(
    status: String,
    message: String,
    result: List[EtherscanTransaction]
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

  // CoinGecko
  case class ConversionResult(
    conversion: String,
    conversionUnit: String,
  )


  // ****** Internal Responses
  case class ErrorResponse(
    message: String,
  )

  case class AddressTypeResponse(
    addressType: AddressType.Value,
  )

  case class Nft(
    tokenAddress: String,
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
        tokenAddress = moralisNft.tokenAddress,
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
    input: String,
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
        input = etherscanTransaction.input,
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
  }

  case class AddressVolumeResponse(
    volumeIn: String,
    volumeOut: String,
  )

  object AddressVolumeResponse {
    def apply(etherscanTransactions: EtherscanTransactions, address: String): AddressVolumeResponse = {

      def sum(txs: List[EtherscanTransaction]): BigInt = txs.foldLeft(BigInt(0)) { case (a, b) => a + BigInt(b.value) }

      val volumeIn = sum(etherscanTransactions.result.filter(_.to == address))
      val volumeOut = sum(etherscanTransactions.result.filter(_.from == address))

      AddressVolumeResponse(
        volumeIn = volumeIn.toString,
        volumeOut = volumeOut.toString,
      )
    }
  }

  case class AddressBalanceResponse(
    balanceETH: Option[Double] = None,
    balanceUSD: Option[Double] = None,
  )

  case class AddressInformationResponse(
    `type`: AddressTypeResponse,
    history: AddressHistoryResponse,
    nfts: NftsResponse,
    volume: AddressVolumeResponse,
  )

}
