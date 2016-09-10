package participant

import akka.actor.{Actor, ActorRef}
import valueAtRisk.OneDayValueAtRiskCalculator.VaR
import akka.pattern.ask
import participant.Client.MarkToMarket

import scala.concurrent.Future
import scala.util.{Failure, Success}

//
//import akka.actor.{Actor, ActorRef, Props}
//import model.Equity
//import model.Market.AddContract
//import participant.Ccp.{Clients, Exposure, Novate, Value}
//import participant.Client.{Add, ExposureTo, Remove}
//
///**
//  * Created by dennis on 19/6/16.
//  */
//case class Ccp(name: String, connections: Set[Connection]) extends Actor {
//  val ccp = context.actorOf(Client.props("CCP"))
//
//  override def receive: Receive = {
//    case Add(contract) => ccp forward Add(contract)
//    case Remove(contract) => ccp forward Remove(contract)
//    case Value => ccp forward Value
//    case Exposure => ccp forward Exposure
//    case ExposureTo(to) => ccp forward ExposureTo(to)
//    case Novate(contract) => novate(contract)
//    case Clients => connections
//  }
//
//  private def novate(contract: Equity) = {
//    contract.market ! AddContract(contract.issuer, contract.buyer, self)
//    contract.market ! AddContract(contract.issuer, self, contract.seller)
//    contract.market ! Remove(contract)
//  }
//}
//
//object Ccp {
//  case class Add(contract: Equity)
//  case class Remove(contract: Equity)
//  case object Value
//  case object Exposure
//  case class ExposureTo(to: ActorRef)
//  case class Novate(contract: Equity)
//  case object Clients
//
//  def props(name: String, connections: Set[Connection]): Props =
//    Props(Ccp(name, connections))
//}

//case class Ccp(name: String, clients: List[ActorRef]) extends Actor {
//  var variationMargins: Map[ActorRef, Option[Double]]
//  var initialMargins: Map[ActorRef, Option[Double]]
//  val equity: Double
//  val guaranteeFunds: Map[ActorRef, Double]
//
//  def initialSetup(client: ActorRef) =  {
//    val valueAtRisk = (client ? VaR).asInstanceOf[Future[Option[Double]]]
//    val markToMarket = (client ? MarkToMarket).asInstanceOf[Future[Option[Double]]]
//
//    valueAtRisk onComplete {
//      case Success(v) => initialMargins = initialMargins + ((client, v))
//      case Failure(e) => println("Error: " + e.getStackTrace)
//    }
//
//    markToMarket onComplete {
//      case Success(v) => variationMargins = variationMargins + ((client, v))
//      case Failure(e) => println("Error: " + e.getStackTrace)
//    }
//  }
//
//  def marginCall(client: ActorRef) = {
//    val markToMarket = (client ? MarkToMarket).asInstanceOf[Future[Option[Double]]]
//
//    markToMarket onComplete {
//      case Success(v) => {
//
//      }
//    }
//  }
//
//}
