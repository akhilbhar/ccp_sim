package participant

import akka.actor.ActorRef
import model.Instrument

/**
  * Performs transactions.
  */
trait ClearingEngine {
  def performTransaction(instrument: Instrument,
                         volume: Long,
                         buyer: ActorRef,
                         seller: ActorRef): Unit
}
