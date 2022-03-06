package app.tilli.api.transaction

import app.tilli.api.utils.SimpleHttpClient
import app.tilli.codec.AddressType
import app.tilli.codec.TilliClasses._
import app.tilli.codec.TilliCodecs._
import cats.effect.IO
import org.http4s.{Header, Headers}
import org.http4s.client.Client
import org.typelevel.ci.CIString

object Calls {

  def addressType(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, AddressTypeResponse]] = {
    val host = "https://api.etherscan.io"
    val apiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"
    val path = "api"
    val queryParams = Map(
      "module" -> "contract",
      "action" -> "getabi",
      "address" -> address,
      "apikey" -> apiKey,
    )

    SimpleHttpClient
      .call[EtherscanContract, AddressTypeResponse](
        host = host,
        path = path,
        queryParams = queryParams,
        conversion = c =>
          AddressTypeResponse(addressType = if (c.status == "1") AddressType.contract else AddressType.external)
      )
  }

  def addressHistory(
    receivingAddress: String,
    sendingAddress: Option[String],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, AddressHistoryResponse]] = {

    val host = "https://api.etherscan.io"
    val apiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"
    val path = s"api"
    val startBlock = "0"
    val endblock = "99999999"
    val page = "1"
    val offset = "10000"
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
      "apikey" -> apiKey,
    )

    SimpleHttpClient
      .call[EtherscanTransactions, AddressHistoryResponse](
        host = host,
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
          AddressHistoryResponse(filteredData)
        }
      )
  }

  def addressNfts(
    address: String,
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, NftsResponse]] = {
    val host = "https://deep-index.moralis.io"
    val apiKey = "gyk7fYMB0EOekZxvsLEDyE0Pm46H6py7iwn0x0fr7ortbcMPmUef0GPnHHtc8upP"
    val path = s"api/v2/$address/nft"
    val queryParams = Map(
      "chain" -> "eth",
    )
    val headers = Headers(Header.Raw(CIString("X-Api-Key"), apiKey))

    SimpleHttpClient
      .call[MoralisNfts, NftsResponse](
        host = host,
        path = path,
        queryParams = queryParams,
        conversion = data => NftsResponse(nfts = data.result.map(Nft(_)).toList),
        headers = headers,
      )
  }

  def addressVolume(
    receivingAddress: String,
    sendingAddress: Option[String],
  )(implicit
    client: Client[IO],
  ): IO[Either[ErrorResponse, AddressVolumeResponse]] = {
    val host = "https://api.etherscan.io"
    val apiKey = "2F4I4U42A674STIFNB4M522BRFSP8MHQHA"
    val path = s"api"
    val startBlock = "0"
    val endblock = "99999999"
    val page = "1"
    val offset = "10000"
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
      "apikey" -> apiKey,
    )

    SimpleHttpClient
      .call[EtherscanTransactions, AddressVolumeResponse](
        host = host,
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
      )
  }

}
