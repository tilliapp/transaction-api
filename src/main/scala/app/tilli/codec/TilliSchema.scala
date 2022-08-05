package app.tilli.codec

import app.tilli.codec.TilliClasses._
import sttp.tapir.{Schema, Validator}

trait TilliSchema {

  // Enums
  implicit val addressTypeSchema = Schema.string.validate(Validator.enumeration(AddressType.values.toList))
  implicit val schemaDimension = Schema.string.validate(Validator.enumeration(Dimension.values.toList))
  implicit val schemaOperator = Schema.string.validate(Validator.enumeration(Operator.values.toList))

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
  implicit lazy val addressTokenSchema: Schema[AddressToken] = Schema.derived
  implicit lazy val addressTokensResponseSchema: Schema[AddressTokensResponse] = Schema.derived
  implicit lazy val listEntrySchema: Schema[ListEntry] = Schema.derived
  implicit lazy val listResponseSchema: Schema[ListResponse] = Schema.derived
  implicit lazy val ensResolutionResponseSchema: Schema[EnsResolutionResponse] = Schema.derived
  implicit lazy val twitterResponseSchema: Schema[TwitterResponse] = Schema.derived
  implicit lazy val twitterResponsesSchema: Schema[TwitterResponses] = Schema.derived

  implicit lazy val schemaAnalyticsResult: Schema[AnalyticsResult] = Schema.derived
  implicit lazy val schemaFilterResponse: Schema[FilterResponse] = Schema.derived
  implicit lazy val schemaFilter: Schema[SimpleFilter] = Schema.derived
  implicit lazy val schemaApiFilters: Schema[FiltersRequest] = Schema.derived

}

object TilliSchema extends TilliSchema
