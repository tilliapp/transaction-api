package app.tilli.codec

import app.tilli.codec.TilliClasses._
import io.circe.Codec
import io.circe.Decoder.decodeEnumeration
import io.circe.Encoder.encodeEnumeration
import io.circe.generic.semiauto.deriveCodec

trait TilliCodecs {

  // External
  implicit lazy val etherScanContractCodec: Codec[EtherscanContract] = deriveCodec

  implicit lazy val moralisNftMetadataCodec: Codec[MoralisNftMetadata] = deriveCodec
  implicit lazy val moralisNftCodec: Codec[MoralisNft] = deriveCodec
  implicit lazy val moralisNftsCodec: Codec[MoralisNfts] = deriveCodec
  implicit lazy val moralisNftTokenUriCodec: Codec[MoralisNftTokenUri] = deriveCodec
  implicit lazy val MoralisDateBlockResponseCodec: Codec[MoralisDateBlockResponse] = deriveCodec


  implicit lazy val etherscanTransactionCodec: Codec[EtherscanTransaction] = deriveCodec
  implicit lazy val etherscanTransactionsCodec: Codec[EtherscanTransactions] = deriveCodec
  implicit lazy val etherscanTokenTransactionCodec: Codec[EtherscanTokenTransaction] = deriveCodec
  implicit lazy val etherscanTokenTransactionsCodec: Codec[EtherscanTokenTransactions] = deriveCodec
  implicit lazy val etherscanBalanceCodec: Codec[EtherscanBalance] = deriveCodec

  // Enums
  implicit lazy val addressTypeDecoder = decodeEnumeration(AddressType)
  implicit lazy val addressTypeEncoder = encodeEnumeration(AddressType)

  // Classes
  implicit lazy val errorResponseCodec: Codec[ErrorResponse] = deriveCodec
  implicit lazy val AddressTypeResponseCodec: Codec[AddressTypeResponse] = deriveCodec
  implicit lazy val nftCodec: Codec[Nft] = deriveCodec
  implicit lazy val nftsResponseCodec: Codec[NftsResponse] = deriveCodec

  implicit lazy val addressHistoryEntryCodec: Codec[AddressHistoryEntry] = deriveCodec
  implicit lazy val addressHistoryCodec: Codec[AddressHistoryResponse] = deriveCodec
  implicit lazy val addressVolumeResponseCodec: Codec[AddressVolumeResponse] = deriveCodec
  implicit lazy val addressInformationResponseCodec: Codec[AddressInformationResponse] = deriveCodec
  implicit lazy val addressAddressBalanceResponseCodec: Codec[AddressBalanceResponse] = deriveCodec

}

object TilliCodecs extends TilliCodecs