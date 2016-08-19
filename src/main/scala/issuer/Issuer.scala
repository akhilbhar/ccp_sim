package issuer

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import issuer.IssuerService.Price

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Issuer of a contract.
  *
  * @param name the name of the issuer.
  * @param service the service handling the issuer.
  */
case class Issuer private[issuer](name: String, service: ActorRef) {
  // TODO not sure
  /**
    * Timeout
    */
  private implicit val timeout = Timeout(5 seconds)

  /**
    * The price of what the issuer is issuing.
    *
    * @return the price of what the issuer is issuing.
    */
  def price: Future[Double] = ask(service, Price(this)).mapTo[Double]
}
