package participant
import akka.actor.ActorRef
import model.{Instrument, Position}
import participant.Client.AddPosition

/**
  * Created by dennis on 20/8/16.
  */
class BilateralClearingEngine extends ClearingEngine {
  override def performTransaction(instrument: Instrument,
                                  volume: Int,
                                  buyer: ActorRef,
                                  seller: ActorRef): Unit = {

//    buyer ! AddPosition(Position(instrument, volume, seller, true))
//    seller ! AddPosition(Position(instrument, volume, buyer, false))

    buyer.tell(AddPosition(Position(instrument, volume, seller, true)), ActorRef.noSender)
    seller.tell(AddPosition(Position(instrument, volume, buyer, false)), ActorRef.noSender)
  }
}
