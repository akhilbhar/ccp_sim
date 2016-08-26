package pricer

import marketFactor.MarketFactor.Price
import marketFactor.{MarketFactor, MarketFactors}
import model.{Equity, Instrument}
import pricer.PricingError.MissingMarketFactors

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, NonEmptyList, \/, \/-}

@implicitNotFound(msg = "No pricer for instrument of type '${I}'.")
trait Pricer[I <: Instrument] {
  def price(instrument: I)(implicit factors: MarketFactors): Future[PricingError \/ Double]
}

object Pricer extends PricerImplicits

trait PricerImplicits {
  implicit object EquityPricer extends Pricer[Equity] {
    override def price(equity: Equity)(
        implicit factors: MarketFactors): Future[PricingError \/ Double] = {
      val priceFactor = Price(equity)

      factors(priceFactor).map(
        _.map(p => \/-(p)).getOrElse(-\/(MissingMarketFactors(NonEmptyList(priceFactor)))))
    }
  }
}

sealed trait PricingError

object PricingError {
  case class MissingMarketFactors(factors: NonEmptyList[MarketFactor]) extends PricingError
}
