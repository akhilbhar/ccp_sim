package custodian

import akka.actor.ActorRef
import instrument.Instrument
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import pricer._

import scala.concurrent.Future
import scalaz.std.FutureInstances
import scalaz.OptionT
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait Position extends MarkToMarket with PricingStrategy with FutureInstances {

  def instrument: Instrument
  def volume: Long
  def counterParty: ActorRef

  override def markToMarket(implicit factors: MarketFactors): Future[Option[Double]] = {
    OptionT(instrument.markToMarket)
      .map(p => {
        this match {
          case LongPosition(_, vol, _) => p * vol
          case ShortPosition(_, vol, _) => -p * vol
        }
      })
      .run
  }

  override def price(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]] = {
    OptionT(instrument.price)
      .map(p => {
        this match {
          case LongPosition(_, vol, _) => p * vol
          case ShortPosition(_, vol, _) => -p * vol
        }
      })
      .run
  }

}

case class LongPosition(instrument: Instrument, volume: Long, counterParty: ActorRef) extends Position
case class ShortPosition(instrument: Instrument, volume: Long, counterParty: ActorRef) extends Position
