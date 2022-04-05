package app.tilli.api.list

import app.tilli.BaseSpec

class TilliListSpec extends BaseSpec {

  "TilliList" must{
    "load" in {
      val Right(blockList) = TilliList.loadTilliBlockList
      blockList.size must be > 100
      println(blockList)
    }
  }

}
