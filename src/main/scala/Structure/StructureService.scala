//package Structure
//
//import Structure.StructureService.{AddCcp, AddClient}
//import akka.actor.{Actor, ActorRef, Props}
//import akka.pattern.ask
//import participant.Ccp.Clients
//import participant.Connection
//
//import scalax.collection.GraphEdge.DiEdge
//import scalax.collection.immutable.Graph
//
///**
//  * Created by dennis on 23/6/16.
//  */
//class StructureService extends Actor {
//  override def receive: Receive = active(Graph.empty)
//
//  private def active(implicit graph: Graph[ActorRef, DiEdge]): Receive = {
//    case AddClient(client) => addClient(client)
//    case AddCcp(ccp) => addCcp(ccp)
//  }
//
//  private def addClient(client: ActorRef)(implicit graph: Graph[ActorRef, DiEdge]) = {
//    context become active(graph + client)
//  }
//
//  private def addCcp(ccp: ActorRef) = {
//    val connections = (ccp ? Clients).mapTo[Set[Connection]]
//
//    // Add ccp
//    // Add clients and connections
//  }
//}
//
//object StructureService {
//  case class AddClient(client: ActorRef)
//  case class AddCcp(ccp: ActorRef)
//
//  def props: Props = Props[StructureService]
//}
