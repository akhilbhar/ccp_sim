package custodian

import akka.actor.ActorRef
import instrument.Instrument
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.{MarketFactors, MarketFactorsError}
import pricer._
import pricingModel.ModelError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.FutureInstances
import scalaz.{EitherT, \/}

sealed trait Position extends FutureInstances {

  def instrument: Instrument
  def volume: Long
  def counterParty: ActorRef

  def markToMarket(implicit factors: MarketFactors): Future[PositionError \/ Double] = {
    EitherT(instrument.markToMarket)
      .map(p => {
        this match {
          case LongPosition(_, vol, _) => p * vol
          case ShortPosition(_, vol, _) => -p * vol
        }
      })
      .leftMap(PositionMarkToMarketError)
      .run
  }

  def price(implicit factors: MarketFactors,
            factorsParameters: MarketFactorsParameters): Future[PositionError \/ Double] = {
    EitherT(instrument.price)
      .map(p => {
        this match {
          case LongPosition(_, vol, _) => p * vol
          case ShortPosition(_, vol, _) => -p * vol
        }
      })
      .leftMap(PositionPricingError)
      .run
  }

}

case class LongPosition(instrument: Instrument, volume: Long, counterParty: ActorRef) extends Position
case class ShortPosition(instrument: Instrument, volume: Long, counterParty: ActorRef) extends Position

sealed trait PositionError
case class PositionPricingError(error: ModelError) extends PositionError
case class PositionMarkToMarketError(error: MarketFactorsError) extends PositionError
