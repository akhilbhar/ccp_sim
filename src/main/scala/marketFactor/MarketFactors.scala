package marketFactor

import marketFactor.MarketFactor.{Price, Volatility}
import model.Equity

import scala.concurrent.Future

/**
  * Created by dennis on 15/8/16.
  */
sealed trait MarketFactor

object MarketFactor {
  case class Price(equity: Equity) extends MarketFactor
  case class Volatility(equity: Equity) extends MarketFactor
}

trait MarketFactors {
  def apply(factor: MarketFactor): Future[Option[Double]] = factor match {
    case Price(equity) => price(equity)
    case Volatility(equity) => volatility(equity)
  }

  protected def price(equity: Equity): Future[Option[Double]]
  protected def volatility(equity: Equity): Future[Option[Double]]
}