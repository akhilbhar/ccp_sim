package participant

import akka.actor.ActorRef

/**
  * Created by dennis on 19/6/16.
  */
sealed trait Connection extends Product with Serializable

final case class ParticipantLink(player: ActorRef) extends Connection
final case class CrossMarginingAgreement(player: ActorRef) extends Connection
