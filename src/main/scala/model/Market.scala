//package model
//
//import akka.actor.{Actor, ActorRef, Props}
//import model.Market.{AddContract, RemoveContract}
//import participant.Client.{Add, Remove}
//
//import scala.collection.immutable.HashSet
//
/////**
////  * Market on which contracts can be issued.
////  *
////  * @param name the name of the market.
////  */
////case class Market(name: String) extends Actor {
////  override def receive: Receive = active(HashSet.empty[Equity])
////
////  private def active(implicit contracts: HashSet[Equity]): Receive = {
////    case AddContract(issuer, buyer, seller) =>
////      sender ! add(issuer, buyer, seller)
////    case RemoveContract(contract) => remove(contract)
////  }
////
////  private def add(issuer: Issuer, buyer: ActorRef, seller: ActorRef)(
////      implicit contracts: HashSet[Equity]) = {
////    val contract = Equity(self, issuer, buyer, seller)
////    context become active(contracts + contract)
////
////    buyer ! Add(contract)
////    seller ! Add(contract)
////
////    contract
////  }
////
////  private def remove(contract: Equity)(
////      implicit contracts: HashSet[Equity]) = {
////    context become active(contracts - contract)
////
////    contract.buyer ! Remove(contract)
////    contract.seller ! Remove(contract)
////  }
////}
////
/////**
////  * Companion object of Market
////  */
////object Market {
////
////  /**
////    * Akka message.
////    * Issues a contract with the provided details and returns the contract.
////    *
////    * @param issuer the issuer of the contract.
////    * @param buyer the buyer of the contract.
////    * @param seller the seller of the contract.
////    */
////  case class AddContract(issuer: Issuer, buyer: ActorRef, seller: ActorRef)
////
////  /**
////    * Akka message.
////    * Remove the contract from the market.
////    *
////    * @param contract the contract to remove.
////    */
////  case class RemoveContract(contract: Equity)
////
////  /**
////    * Creates a prop with an instance of the market.
////    *
////    * Use this to add the actor.
////    *
////    * @param name the name of market.
////    * @return the prop with the instance of the market.
////    */
////  def props(name: String): Props = Props(Market(name))
////}
