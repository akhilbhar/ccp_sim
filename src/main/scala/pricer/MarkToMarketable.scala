package pricer

import marketFactor.MarketFactors

import scala.concurrent.Future

/**
  * Created by dennis on 5/9/16.
  */
trait MarkToMarket {
  def markToMarket(implicit factors: MarketFactors): Future[Option[Double]]
}
