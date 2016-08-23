package pricer

import marketFactor.MarketFactors
import model._
import pricer.PortfolioPricingError.UnderlyingPricingErrors

import scalaz.Scalaz._
import scalaz.{-\/, NonEmptyList, \/, \/-}

/**
  * Created by dennis on 15/8/16.
  */
object PortfolioPricer {
  def price(portfolio: Portfolio)(
      implicit factors: MarketFactors): PortfolioPricingError \/ Double = {
    def markToMarket[I <: Instrument](instrument: I)(
        implicit pricer: Pricer[I]): PricingError \/ Double = {
      pricer.price(instrument)
    }

    val mtm: List[PricingError \/ Double] = portfolio.positions map {
      case Position(instrument, volume, _, true) => markToMarket(instrument).map(_ * volume)
      case Position(instrument, volume, _, false) => markToMarket(instrument).map(-_ * volume)
    }

    val total: NonEmptyList[PricingError] \/ Double = \/.fromEither(
      mtm.map(_.validation.toValidationNel).reduceLeft((l, r) => (l |@| r)(_ + _)).toEither
    )

    total.fold(
      error => -\/(UnderlyingPricingErrors(error.stream.toVector)),
      success => \/-(success)
    )
  }
}

sealed trait PortfolioPricingError

object PortfolioPricingError {
  case class UnderlyingPricingErrors(errors: Vector[PricingError]) extends PortfolioPricingError
}
