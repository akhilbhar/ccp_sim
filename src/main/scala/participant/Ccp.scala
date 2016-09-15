//package participant
//
//import akka.actor.Actor.Receive
//import akka.actor.{Actor, ActorRef, Props}
//import valueAtRisk.ValueAtRiskCalculator.VaR
//import akka.pattern.ask
//import participant.Ccp.{Default, MarginCall, ProvideMargin}
//import participant.Client.MarkToMarket
//import pricer.MarkToMarketError
//
//import scala.concurrent.Future
//import scala.util.{Failure, Success}
//import scalaz.\/
//
////
////import akka.actor.{Actor, ActorRef, Props}
////import model.Equity
////import model.Market.AddContract
////import participant.Ccp.{Clients, Exposure, Novate, Value}
////import participant.Client.{Add, ExposureTo, Remove}
////
/////**
////  * Created by dennis on 19/6/16.
////  */
////case class Ccp(name: String, connections: Set[Connection]) extends Actor {
////  val ccp = context.actorOf(Client.props("CCP"))
////
////  override def receive: Receive = {
////    case Add(contract) => ccp forward Add(contract)
////    case Remove(contract) => ccp forward Remove(contract)
////    case Value => ccp forward Value
////    case Exposure => ccp forward Exposure
////    case ExposureTo(to) => ccp forward ExposureTo(to)
////    case Novate(contract) => novate(contract)
////    case Clients => connections
////  }
////
////  private def novate(contract: Equity) = {
////    contract.market ! AddContract(contract.issuer, contract.buyer, self)
////    contract.market ! AddContract(contract.issuer, self, contract.seller)
////    contract.market ! Remove(contract)
////  }
////}
////
////object Ccp {
////  case class Add(contract: Equity)
////  case class Remove(contract: Equity)
////  case object Value
////  case object Exposure
////  case class ExposureTo(to: ActorRef)
////  case class Novate(contract: Equity)
////  case object Clients
////
////  def props(name: String, connections: Set[Connection]): Props =
////    Props(Ccp(name, connections))
////}
//
//
//// TODO add equity
//// TODO add guarantee fund
//class Ccp(name: String,
//          clients: List[ActorRef],
//          variationMargins: Map[ActorRef, Future[Option[Double]]],
//          initialMargins: Map[ActorRef, Future[Option[Double]]])
//    extends Actor {
//
//  override def receive: Receive = {
//    case Default => default(sender)
//    case ProvideMargin(m) =>
//  }
//
//
//  private def default(client: ActorRef) = {
//    val markToMarketPortfolio = (client ? MarkToMarket).mapTo[MarkToMarketError \/ Double]
//
//    // Sell portfolio at MtM.
//    // What is left is removed from margins
//    // Whats is left is removed from guarantee fund
//    // Left ? CCP default ?
//  }
//
//  private def marginCall(client: ActorRef) = {
//
//  }
//
//}
//
//object Ccp {
//  def apply(name: String, clients: List[ActorRef]) = {
//    val initialMargins: Map[ActorRef, Future[Option[Double]]] =
//      clients.map(client => client -> (client ? VaR).mapTo[Option[Double]])(scala.collection.breakOut)
//
//    val variationMargins: Map[ActorRef, Future[Option[Double]]] =
//      clients.map(client => client -> (client ? MarkToMarket).mapTo[Option[Double]])(scala.collection.breakOut)
//
//    new Ccp(name, clients, initialMargins, variationMargins)
//  }
//
//  def props(name: String, clients: List[ActorRef]) = Props(Ccp(name, clients))
//
//  /* ---------------------------------------------------------------------------------------------*/
//
//  case class ProvideMargin(margin: Double)
//  case object Default
//}
