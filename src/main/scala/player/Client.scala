package player
import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import model.Portfolio
import player.Client._
import pricer.{PortfolioPricer, PortfolioPricingError}

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scalaz.\/

/**
  * Created by dennis on 19/6/16.
  */
case class Client(name: String, portfolio: Portfolio) extends Actor {
  // TODO wuuut
  import context._
  val id = UUID.randomUUID().toString

  override def receive: Receive = {
    case Value => sender ! value
    case Exposure => sender ! exposure
    case ExposureTo(to) => sender ! exposureTo(to)
  }

  def value: PortfolioPricingError \/ Double = {
    PortfolioPricer.price(portfolio)
  }

  private def exposureTo(to: ActorRef)(
      implicit exposures: HashMap[ActorRef, Future[Double]]) = {
    exposures.get(to) match {
      case Some(exposure) => exposure
      case None => Future.successful(0.0)
    }
  }

  private def exposure(implicit exposures: HashMap[ActorRef, Future[Double]]) = {
    Future.sequence(exposures.values).map(_.sum)
  }
}

object Client {
  case object Value
  case object Exposure
  case class ExposureTo(to: ActorRef)

  def props(name: String, portfolio: Portfolio): Props =
    Props(Client(name, portfolio))
}
