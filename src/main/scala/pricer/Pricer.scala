package pricer

import marketFactor.MarketFactor.Price
import marketFactor.{MarketFactor, MarketFactors}
import model.{Equity, Instrument}

import scala.annotation.implicitNotFound
import scalaz.{-\/, NonEmptyList, \/, \/-}

sealed trait PricingError

object PricingError {
  case class MissingMarketFactors(factors: NonEmptyList[MarketFactor]) extends PricingError
}

@implicitNotFound(msg = "No pricer for instrument of type '${I}'.")
trait Pricer[I <: Instrument] {
  def price(instrument: I)(implicit factors: MarketFactors): PricingError \/ Double
}

object Pricer extends PricerImplicits

trait PricerImplicits {
  sealed trait PricingError
  case class MissingFactors(factors: NonEmptyList[MarketFactor]) extends PricingError

  implicit object EquityPricer extends Pricer[Equity] {
    def price(equity: Equity)(implicit factors: MarketFactors): PricingError \/ Double = {
      val priceFactor = Price(equity)

      factors(priceFactor) map (v => \/-(v)) getOrElse -\/(
        MissingFactors(NonEmptyList(priceFactor)))
    }
  }
}
