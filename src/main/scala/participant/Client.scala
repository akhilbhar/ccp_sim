package participant

import java.util.Calendar

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe
import marketFactor.MarketFactorsBuilder
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.{Portfolio, Position}
import participant.Client._
import pricer.{PortfolioPricer, PortfolioPricingError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.\/

/**
  * Created by dennis on 19/6/16.
  */
case class Client(name: String, marketFactorsBuilder: MarketFactorsBuilder) extends Actor {
  private var portfolio: Portfolio = Portfolio(List())

  override def receive: Receive = {
    case Value => pipe(value) to sender
    case AddPosition(p) => addPosition(p)
  }

  private def value: Future[PortfolioPricingError \/ Double] = {
    val date = Calendar.getInstance
    date.add(Calendar.DATE, -1)
    implicit val factors = marketFactorsBuilder.marketFactors(date)(new MarketFactorsParameters)

    PortfolioPricer.price(portfolio)
  }

  private def addPosition(position: Position): Unit = {
    portfolio = portfolio.addPosition(position)
    println("Added " + position.instrument)
  }
}

object Client {

  def props(name: String, marketFactorsBuilder: MarketFactorsBuilder): Props =
    Props(Client(name, marketFactorsBuilder))

  case class ExposureTo(to: ActorRef)

  case class AddPosition(position: Position)

  case object Value

  case object Exposure
}
