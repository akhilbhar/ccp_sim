package player

import akka.actor.{Actor, ActorRef, Props}
import model.Equity
import model.Market.AddContract
import player.Ccp.{Clients, Exposure, Novate, Value}
import player.Client.{Add, ExposureTo, Remove}

/**
  * Created by dennis on 19/6/16.
  */
case class Ccp(name: String, connections: Set[Connection]) extends Actor {
  val ccp = context.actorOf(Client.props("CCP"))

  override def receive: Receive = {
    case Add(contract) => ccp forward Add(contract)
    case Remove(contract) => ccp forward Remove(contract)
    case Value => ccp forward Value
    case Exposure => ccp forward Exposure
    case ExposureTo(to) => ccp forward ExposureTo(to)
    case Novate(contract) => novate(contract)
    case Clients => connections
  }

  private def novate(contract: Equity) = {
    contract.market ! AddContract(contract.issuer, contract.buyer, self)
    contract.market ! AddContract(contract.issuer, self, contract.seller)
    contract.market ! Remove(contract)
  }
}

object Ccp {
  case class Add(contract: Equity)
  case class Remove(contract: Equity)
  case object Value
  case object Exposure
  case class ExposureTo(to: ActorRef)
  case class Novate(contract: Equity)
  case object Clients

  def props(name: String, connections: Set[Connection]): Props =
    Props(Ccp(name, connections))
}
