package model

import akka.actor.ActorRef

sealed trait Position {
  def instrument: Instrument
}

case class LongPosition(instrument: Instrument, volume: Long, counterParty: ActorRef) extends Position
case class ShortPosition(instrument: Instrument, volume: Long, counterParty: ActorRef) extends Position

