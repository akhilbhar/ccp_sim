package marketFactor

import java.util.Calendar

import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.Portfolio

import scala.concurrent.Future

/**
  * Provides market factors for the valuation.
  */
trait MarketFactorsBuilder {

  /**
    * Creates a generator of market factors based on a Monte-Carlo simulation.
    *
    * @param portfolio portfolio for which to build a predictive generator
    * @param date date until which data is used
    * @param parameters parameters for the computation
    * @return a generator of forecasted market factors
    */
  def oneDayForecastMarketFactors(portfolio: Portfolio, date: Calendar)(
      implicit parameters: MarketFactorsParameters): Future[MarketFactorsGenerator]

  /**
    * Provides market factors for the provided date
    * @param date date of the market factors
    * @param parameters parameters for the computation
    * @return market factors for the provided date
    */
  def marketFactors(date: Calendar)(implicit parameters: MarketFactorsParameters): MarketFactors
}

object MarketFactorsBuilder {

  /**
    * Parameters to be used for building the market factors.
    * @param riskFreeRate
    * @param horizon Real of days of data to use.
    */
  case class MarketFactorsParameters(riskFreeRate: Double = 0, horizon: Long = 1000)

}
