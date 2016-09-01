package marketFactor

import java.util.Calendar

import marketFactor.MarketFactor.{DaysToMaturity, Price, RiskFreeRate, Volatility}
import model.Equity
import spire.math.Real

import scala.concurrent.Future

sealed trait MarketFactor

object MarketFactor {
  case class Price(equity: Equity) extends MarketFactor
  case class Volatility(equity: Equity) extends MarketFactor
  case class DaysToMaturity(maturity: Calendar) extends MarketFactor
  case object RiskFreeRate extends MarketFactor
}

/**
  * Provides the value of market factors.
  */
trait MarketFactors {
  def apply(factor: MarketFactor): Future[Option[Real]] = factor match {
    case Price(equity) => price(equity)
    case Volatility(equity) => volatility(equity)
    case DaysToMaturity(maturity) => daysToMaturity(maturity)
    case RiskFreeRate => riskFreeRate
  }

  protected def price(equity: Equity): Future[Option[Real]]
  protected def volatility(equity: Equity): Future[Option[Real]]
  protected def daysToMaturity(maturity: Calendar): Future[Option[Real]]
  protected def riskFreeRate: Future[Option[Real]]
}
