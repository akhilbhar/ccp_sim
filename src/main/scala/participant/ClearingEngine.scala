package participant

import akka.actor.ActorRef
import model.Instrument

/**
  * Performs transactions.
  */
trait ClearingEngine {
  def performTransaction(instrument: Instrument,
                         volume: Int,
                         buyer: ActorRef,
                         seller: ActorRef): Unit
}
