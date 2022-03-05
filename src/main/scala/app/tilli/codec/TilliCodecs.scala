package app.tilli.codec

import app.tilli.codec.TilliClasses._
import io.circe.Codec
import io.circe.Decoder.decodeEnumeration
import io.circe.Encoder.encodeEnumeration
import io.circe.generic.semiauto.deriveCodec

trait TilliCodecs {

  // External
  implicit lazy val etherScanContractCodec: Codec[EtherScanContract] = deriveCodec

  implicit lazy val moralisNftMetadataCodec: Codec[MoralisNftMetadata] = deriveCodec
  implicit lazy val moralisNftCodec: Codec[MoralisNft] = deriveCodec
  implicit lazy val moralisNftsCodec: Codec[MoralisNfts] = deriveCodec
  implicit lazy val moralisNftTokenUri: Codec[MoralisNftTokenUri] = deriveCodec

  // Enums
  implicit lazy val addressTypeDecoder = decodeEnumeration(AddressType)
  implicit lazy val addressTypeEncoder = encodeEnumeration(AddressType)

  // Classes
  implicit lazy val errorResponseCodec: Codec[ErrorResponse] = deriveCodec
  implicit lazy val AddressTypeResponseCodec: Codec[AddressTypeResponse] = deriveCodec
  implicit lazy val nftCodec: Codec[Nft] = deriveCodec
  implicit lazy val nftsResponseCodec: Codec[NftsResponse] = deriveCodec

}

object TilliCodecs extends TilliCodecs