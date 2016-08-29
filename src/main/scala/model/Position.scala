package model

import akka.actor.ActorRef

case class Position(instrument: Instrument, volume: Int, counterParty: ActorRef, long: Boolean)
