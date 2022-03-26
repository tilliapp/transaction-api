package app.tilli.codec

import app.tilli.codec.TilliClasses._
import sttp.tapir.{Schema, Validator}

trait TilliSchema {

  import io.circe.generic.auto._

  // Enums
  implicit val addressTypeSchema = Schema.string.validate(Validator.enumeration(AddressType.values.toList))

  // External Classes
  implicit lazy val etherscanContractSchema: Schema[EtherscanContract] = Schema.derived
  implicit lazy val moralisNftMetadataSchema: Schema[MoralisNftMetadata] = Schema.derived
  implicit lazy val moralisNftSchema: Schema[MoralisNft] = Schema.derived
  implicit lazy val moralisNftsSchema: Schema[MoralisNfts] = Schema.derived

  // Classes
  implicit lazy val addressTypeResponseSchema: Schema[AddressTypeResponse] = Schema.derived
  implicit lazy val nftSchema: Schema[Nft] = Schema.derived
  implicit lazy val nftsResponseSchema: Schema[NftsResponse] = Schema.derived
  implicit lazy val errorResponseSchema: Schema[ErrorResponse] = Schema.derived
  implicit lazy val addressHistoryEntrySchema: Schema[AddressHistoryEntry] = Schema.derived
  implicit lazy val addressHistorySchema: Schema[AddressHistoryResponse] = Schema.derived
  implicit lazy val addressVolumeResponseSchema: Schema[AddressVolumeResponse] = Schema.derived
  implicit lazy val addressInformationResponseSchema: Schema[AddressInformationResponse] = Schema.derived

  implicit lazy val addressAddressBalanceResponseSchema: Schema[AddressBalanceResponse] = Schema.derived

}

object TilliSchema extends TilliSchema
