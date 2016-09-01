package model

import akka.actor.ActorRef
import spire.math.SafeLong

sealed trait Position {
  def instrument: Instrument
}

case class LongPosition(instrument: Instrument, volume: SafeLong, counterParty: ActorRef) extends Position
case class ShortPosition(instrument: Instrument, volume: SafeLong, counterParty: ActorRef) extends Position

