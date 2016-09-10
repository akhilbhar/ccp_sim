package pricer

import marketFactor.MarketFactors

import scala.concurrent.Future
import scalaz.\/

/**
  * Created by dennis on 5/9/16.
  */
trait MarkToMarketable {
  def markToMarket(implicit factors: MarketFactors): Future[MarkToMarketError \/ Double]
}

sealed trait MarkToMarketError
