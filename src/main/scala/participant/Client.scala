package participant
import akka.actor.{Actor, ActorRef, Props}
import marketFactor.MarketFactorsBuilder
import model.{Portfolio, Position}
import participant.Client._
import pricer.PortfolioPricingError

import scalaz.\/

/**
  * Created by dennis on 19/6/16.
  */
case class Client(name: String) extends Actor {
  // TODO wuuut
  // private val id = UUID.randomUUID().toString
  private var portfolio: Portfolio = Portfolio(List())

  override def receive: Receive = {
    case Value => sender ! value
//    case Exposure => sender ! exposure
//    case ExposureTo(to) => sender ! exposureTo(to)
    case AddPosition(p) => addPosition(p)
  }

  def value: PortfolioPricingError \/ Double = {
    val generator = MarketFactorsBuilder.
    PortfolioPricer.price(portfolio)
  }

//  private def exposureTo(to: ActorRef)(
//      implicit exposures: HashMap[ActorRef, Future[Double]]) = {
//    exposures.get(to) match {
//      case Some(exposure) => exposure
//      case None => Future.successful(0.0)
//    }
//  }
//
//  private def exposure(implicit exposures: HashMap[ActorRef, Future[Double]]) = {
//    Future.sequence(exposures.values).map(_.sum)
//  }

  private def addPosition(position: Position): Unit = {
    portfolio = portfolio.addPosition(position)
  }
}

object Client {
  case object Value
  case object Exposure
  case class ExposureTo(to: ActorRef)
  case class AddPosition(position: Position)

  def props(name: String, portfolio: Portfolio): Props =
    Props(Client(name))
}
