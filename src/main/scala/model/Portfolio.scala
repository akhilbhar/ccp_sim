package model

import akka.actor.ActorRef

import scalaz.NonEmptyList

/**
  * Created by dennis on 15/8/16.
  */
sealed trait Position

case class LongPosition(instrument: Instrument,
                        volume: Int,
                        counterParty: ActorRef) extends Position

case class ShortPosition(instrument: Instrument,
                         volume: Int,
                         counterParty: ActorRef) extends Position

case class Portfolio(positions: NonEmptyList[Position])

sealed trait PortfolioError

object PortfolioError {
  case class InvalidPosition(position: Position)
}
