package pricingModel.equity

import instrument.Equity
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import pricingModel.{MissingMarketFactors, ModelError, PricingModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.FutureInstances
import scalaz.{EitherT, NonEmptyList, \/}

/**
  * Created by dennis on 9/9/16.
  */
object IdentityModel extends PricingModel[Equity] with FutureInstances {
  override def price(equity: Equity)(implicit factors: MarketFactors,
                                     factorsParameters: MarketFactorsParameters): Future[\/[ModelError, Double]] = {
    EitherT(equity.markToMarket).leftMap(NonEmptyList(_)).leftMap(MissingMarketFactors).run
  }
}
