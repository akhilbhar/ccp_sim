package participant

import akka.actor.ActorRef
import model.{Instrument, LongPosition, ShortPosition}
import participant.Client.AddPosition
import spire.math.SafeLong

/**
  * Performs a bilateral transaction.
  */
case object BilateralClearingEngine extends ClearingEngine {
  override def performTransaction(instrument: Instrument,
                                  volume: SafeLong,
                                  buyer: ActorRef,
                                  seller: ActorRef): Unit = {
    buyer.tell(AddPosition(LongPosition(instrument, volume, seller)), ActorRef.noSender)
    seller.tell(AddPosition(ShortPosition(instrument, volume, buyer)), ActorRef.noSender)
  }
}
