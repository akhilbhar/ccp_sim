package pricer

import marketFactor.MarketFactors
import model._
import pricer.PortfolioPricingError.UnderlyingPricingErrors

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, EitherT, NonEmptyList, \/, \/-}

/**
  * Created by dennis on 15/8/16.
  */
object PortfolioPricer {
  def price(portfolio: Portfolio)(
      implicit factors: MarketFactors): Future[PortfolioPricingError \/ Double] = {
    def markToMarket[I <: Instrument](instrument: I)(
        implicit pricer: Pricer[I]): Future[PricingError \/ Double] = {
      pricer.price(instrument)
    }

    val mtmF: Future[List[PricingError \/ Double]] = Future.sequence(portfolio.positions map {
      case Position(instrument, volume, _, true) =>
        (for { price <- EitherT(markToMarket(instrument)) } yield price * volume).run
      case Position(instrument, volume, _, false) =>
        (for { price <- EitherT(markToMarket(instrument)) } yield -price * volume).run
    })

    val totalF: Future[NonEmptyList[PricingError] \/ Double] = {
      for {
        mtm <- mtmF
      } yield
        \/.fromEither(
          mtm.map(_.validation.toValidationNel).reduceLeft((l, r) => (l |@| r)(_ + _)).toEither)
    }

    for {
      total <- totalF
    } yield
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
