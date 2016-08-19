package pricer

import model._
import pricer.PortfolioPricingError.UnderlyingPricingErrors

import scalaz.{-\/, NonEmptyList, \/, \/-}
import scalaz.Scalaz._

/**
  * Created by dennis on 15/8/16.
  */
object PortfolioPricer {
  def price(portfolio: Portfolio)(
      implicit factors: Factors): PortfolioPricingError \/ Double = {
    def markToMarket[I <: Instrument](instrument: I)(
        implicit pricer: Pricer[I]): PricingError \/ Double = {
      pricer.price(instrument)
    }

    val mtm: NonEmptyList[PricingError \/ Double] = portfolio.positions map {
      case LongPosition(instrument: Instrument, volume: Int, _) =>
        markToMarket(instrument).map(_ * volume)
      case ShortPosition(instrument: Instrument, volume: Int, _) =>
        markToMarket(instrument).map(-_ * volume)
    }

    val total: NonEmptyList[PricingError] \/ Double = \/.fromEither(
        mtm
          .map(_.validation.toValidationNel)
          .stream
          .reduceLeft((l, r) => (l |@| r)(_ + _))
          .toEither
    )

    total.fold(
        error => -\/(UnderlyingPricingErrors(error.stream.toVector)),
        success => \/-(success)
    )
  }
}

sealed trait PortfolioPricingError

object PortfolioPricingError {
  case class UnderlyingPricingErrors(errors: Vector[PricingError])
      extends PortfolioPricingError
}
