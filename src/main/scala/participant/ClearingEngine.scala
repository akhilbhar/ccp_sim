package participant

import akka.actor.ActorRef
import model.Instrument
import spire.math.SafeLong

/**
  * Performs transactions.
  */
trait ClearingEngine {
  def performTransaction(instrument: Instrument,
                         volume: SafeLong,
                         buyer: ActorRef,
                         seller: ActorRef): Unit
}
