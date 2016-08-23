package model

import akka.actor.ActorRef

/**
  * Created by dennis on 15/8/16.
  */
case class Position(instrument: Instrument, volume: Int, counterParty: ActorRef, long: Boolean)
