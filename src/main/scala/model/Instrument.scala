package model

import data.DataSource
import marketFactor.MarketFactor.Price
import marketFactor.MarketFactors
import pricer.{MarkToMarket, PricingStrategy}

import scala.concurrent.Future

trait Instrument extends DataSource with PricingStrategy with MarkToMarket {
  override def markToMarket(implicit factors: MarketFactors): Future[Option[Double]] = {
    val priceFactor = Price(this)

    /* Gets the price factor from the market factors */
    factors(priceFactor)
  }
}
