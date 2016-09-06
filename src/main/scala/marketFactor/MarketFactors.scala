package marketFactor

import java.util.Calendar

import marketFactor.MarketFactor.{DaysToMaturity, Price, RiskFreeRate, Volatility}
import model.Instrument

import scala.concurrent.Future

sealed trait MarketFactor

object MarketFactor {

  case class Price(instrument: Instrument) extends MarketFactor

  case class Volatility(instrument: Instrument) extends MarketFactor

  case class DaysToMaturity(maturity: Calendar) extends MarketFactor

  case object RiskFreeRate extends MarketFactor

}

/**
  * Provides the value of market factors.
  */
trait MarketFactors {

  def apply(factor: MarketFactor): Future[Option[Double]] = factor match {
    case Price(instrument) => price(instrument)
    case Volatility(instrument) => volatility(instrument)
    case DaysToMaturity(maturity) => daysToMaturity(maturity)
    case RiskFreeRate => riskFreeRate
  }

  protected def price(instrument: Instrument): Future[Option[Double]]

  protected def volatility(instrument: Instrument): Future[Option[Double]]

  protected def daysToMaturity(maturity: Calendar): Future[Option[Double]]

  protected def riskFreeRate: Future[Option[Double]]

}
