package participant

import akka.actor.ActorRef
import model.{Instrument, Position}
import participant.Client.AddPosition

/**
  * Performs a bilateral transaction.
  */
case object BilateralClearingEngine extends ClearingEngine {
  override def performTransaction(instrument: Instrument,
                                  volume: Int,
                                  buyer: ActorRef,
                                  seller: ActorRef): Unit = {
    buyer.tell(AddPosition(Position(instrument, volume, seller, true)), ActorRef.noSender)
    seller.tell(AddPosition(Position(instrument, volume, buyer, false)), ActorRef.noSender)
  }
}
