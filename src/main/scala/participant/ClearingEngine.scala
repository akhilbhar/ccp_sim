package participant

import akka.actor.ActorRef
import model.Instrument

/**
  * Created by dennis on 20/8/16.
  */
trait ClearingEngine {
  def performTransaction(instrument: Instrument,
                         volume: Int,
                         buyer: ActorRef,
                         seller: ActorRef): Unit
}
