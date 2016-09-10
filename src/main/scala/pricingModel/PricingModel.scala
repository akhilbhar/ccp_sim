package pricingModel

import instrument.Instrument
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.{MarketFactors, MarketFactorsError}

import scala.concurrent.Future
import scalaz.{NonEmptyList, \/}

/**
  * Created by dennis on 8/9/16.
  */
trait PricingModel[-I <: Instrument] {
  def price(instrument: I)(implicit factors: MarketFactors,
                           factorsParameters: MarketFactorsParameters): Future[ModelError \/ Double]
}

sealed trait ModelError
case class MissingMarketFactors(errors: NonEmptyList[MarketFactorsError]) extends ModelError