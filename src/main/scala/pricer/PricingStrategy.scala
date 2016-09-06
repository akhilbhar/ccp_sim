package pricer

import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters

import scala.concurrent.Future

trait PricingStrategy {
  def price(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]]
}
