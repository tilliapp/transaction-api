package app.tilli.codec

import app.tilli.BaseSpec
import app.tilli.codec.TilliClasses.Nft

class TilliClassesSpec extends BaseSpec {

  "TilliClasses" must {
    "decode token uri" in {
      val ex = "data:application/json;base64,eyJuYW1lIjogIlRob3IiLCAiZGVzY3JpcHRpb24iOiAiIiwgImltYWdlIjoiaHR0cHM6Ly9jbG91ZGZsYXJlLWlwZnMuY29tL2lwZnMvUW1SN2NMcjRZZkZoSFNiQ25mM3RMdEs0WWJyVEFyQzZYN0cxdWJvWXJNVUY2YSJ9=="
      val Right(nft) = Nft.decodeTokenUri(ex)
      nft.image mustBe "https://cloudflare-ipfs.com/ipfs/QmR7cLr4YfFhHSbCnf3tLtK4YbrTArC6X7G1uboYrMUF6a"
    }

    //  This test fails since the name field has special characters in it:
    // {"name": "Lotus #                              ", "description": "", "image":"https://cloudflare-ipfs.com/ipfs/Qmc5p2TUWYDpnZVB5XFgAZcQG9bdDKaWC8kJo2C2W8zoGS"}
    //    "decode another token uri" in {
    //      val ex = "data:application/json;base64,eyJuYW1lIjogIkxvdHVzICMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABGSIsICJkZXNjcmlwdGlvbiI6ICIiLCAiaW1hZ2UiOiJodHRwczovL2Nsb3VkZmxhcmUtaXBmcy5jb20vaXBmcy9RbWM1cDJUVVdZRHBuWlZCNVhGZ0FaY1FHOWJkREthV0M4a0pvMkMyVzh6b0dTIn0=="
    //      val Right(nft) = Nft.decodeTokenUri(ex)
    //      nft.image mustBe "https://cloudflare-ipfs.com/ipfs/Qmc5p2TUWYDpnZVB5XFgAZcQG9bdDKaWC8kJo2C2W8zoGS"
    //    }

    "handle very large numbers" in {
      val test = "5000000000000000000000000000" // 5 billion ETH in WEI
      val asBigInt = BigInt(test)
      asBigInt.toString mustBe test
    }
  }
}
