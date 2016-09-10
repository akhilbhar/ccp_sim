package marketFactor

import data.DataError
import instrument.{EquityOption, Instrument}
import marketFactor.MarketFactor._

import scala.concurrent.Future
import scalaz.\/

sealed trait MarketFactor

object MarketFactor {

  case class Price(instrument: Instrument) extends MarketFactor

  case class Volatility(instrument: Instrument) extends MarketFactor

  case class Mean(instrument: Instrument) extends MarketFactor

  case class DaysToMaturity(option: EquityOption) extends MarketFactor

  case class DividendYield(instrument: Instrument) extends MarketFactor

  case object RiskFreeRate extends MarketFactor

}

/**
  * Provides the value of market factors.
  */
trait MarketFactors {

  def apply(factor: MarketFactor): Future[MarketFactorsError \/ Double] = factor match {
    case Price(instrument) => price(instrument)
    case Volatility(instrument) => volatility(instrument)
    case Mean(instrument) => mean(instrument)
    case DaysToMaturity(maturity) => daysToMaturity(maturity)
    case DividendYield(instrument) => dividendYield(instrument)
    case RiskFreeRate => riskFreeRate
  }

  protected def price(instrument: Instrument): Future[MarketFactorsError \/ Double]

  protected def volatility(instrument: Instrument): Future[MarketFactorsError \/ Double]

  protected def mean(instrument: Instrument): Future[MarketFactorsError \/ Double]

  protected def daysToMaturity(option: EquityOption): Future[MarketFactorsError \/ Double]

  protected def dividendYield(instrument: Instrument): Future[MarketFactorsError \/ Double]

  protected def riskFreeRate: Future[MarketFactorsError \/ Double]

}

sealed trait MarketFactorsError
case class MissingData(error: DataError) extends MarketFactorsError
case class ExpiredOption(instrument: Instrument) extends MarketFactorsError
