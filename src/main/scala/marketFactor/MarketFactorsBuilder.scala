package marketFactor

import java.time.LocalDate

import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.Portfolio

/**
  * Created by dennis on 21/8/16.
  */
trait MarketFactorsBuilder {
  def oneDayForecastMarketFactors(portfolio: Portfolio,
                                  date: LocalDate,
                                  parameters: MarketFactorsParameters): MarketFactorsGenerator

  def marketFactors(date: LocalDate, parameters: MarketFactorsParameters): MarketFactors
}

object MarketFactorsBuilder {
  case class MarketFactorsParameters(riskFreeRate: Double = 0, horizon: Int = 1000)
}
