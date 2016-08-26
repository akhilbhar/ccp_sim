package marketFactor

import java.time.LocalDate

import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.Portfolio

import scala.concurrent.Future

/**
  * Created by dennis on 21/8/16.
  */
trait MarketFactorsBuilder {
  def oneDayForecastMarketFactors(portfolio: Portfolio, date: LocalDate)(
    implicit parameters: MarketFactorsParameters): Future[MarketFactorsGenerator]

  def marketFactors(date: LocalDate)(
    implicit parameters: MarketFactorsParameters): MarketFactors
}

object MarketFactorsBuilder {

  case class MarketFactorsParameters(riskFreeRate: Double = 0, horizon: Int = 1000)

}
