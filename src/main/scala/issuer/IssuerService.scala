package issuer

import akka.actor.{Actor, Props}
import breeze.stats.distributions.ContinuousDistr
import issuer.IssuerService.{AddIssuer, Price, RemoveIssuer}

import scala.collection.immutable.HashMap

/**
  * Service handling the prices of the issuances of issuers.
  *
  * @param name the name of the service.
  */
case class IssuerService(name: String, periods: Int) extends Actor {

  override def receive: Receive = active(HashMap.empty, HashMap.empty)

  private def active(
      implicit issuers: HashMap[Issuer, ContinuousDistr[Double]],
      prices: HashMap[Issuer, Seq[Double]]): Receive = {
    case AddIssuer(n, priceDistr) => sender ! add(n, priceDistr)
    case RemoveIssuer(issuer) => remove(issuer)
    case Price(issuer, time) => sender ! price(issuer, time)
  }

  private def add(name: String, priceDistr: ContinuousDistr[Double])(
      implicit issuers: HashMap[Issuer, ContinuousDistr[Double]],
      prices: HashMap[Issuer, IndexedSeq[Double]]): Issuer = {
    val issuer = Issuer(name, self)
    val price = priceDistr.sample(periods)

    // Just overwrites the previous entry
    context become active(issuers + (issuer -> priceDistr),
                          prices + (issuer -> price))

    issuer
  }

  private def remove(issuer: Issuer)(
      implicit issuers: HashMap[Issuer, ContinuousDistr[Double]],
      prices: HashMap[Issuer, IndexedSeq[Double]]) = {
    context become active(issuers - issuer, prices - issuer)
  }

  private def price(issuer: Issuer, time: Int)(
      implicit issuers: HashMap[Issuer, ContinuousDistr[Double]],
      prices: HashMap[Issuer, IndexedSeq[Double]]) = {
    val price = prices.get(issuer) match {
      case Some(ps) =>
        if (ps isDefinedAt time) {
          ps(time)
        } else {
          None
        }
      case _ => _
    }

    price
  }
}

/**
  * Companion object of IssuerService
  */
object IssuerService {

  /**
    * Akka message.
    * Adds an issuer and returns it.
    *
    * @param name the name of the issuer.
    * @param priceDistr the distribution of the price of the issuer's issuance.
    */
  case class AddIssuer(name: String, priceDistr: ContinuousDistr[Double])

  /**
    * Akka message.
    *
    * Removes the issuer.
    *
    * @param issuer the issuer to remove.
    */
  case class RemoveIssuer(issuer: Issuer)

  /**
    * Akka message.
    *
    * The price of issuer's issuance.
    *
    * @param issuer the issuer
    * @param time price at this time
    */
  case class Price(issuer: Issuer, time: Int)

  /**
    * Creates a prop with an instance of the issuer service.
    *
    * Use this to add the actor.
    *
    * @param name the name of service.
    * @return the prop with the instance of the service.
    */
  def props(name: String, periods: Int): Props = Props(IssuerService(name, periods))
}
