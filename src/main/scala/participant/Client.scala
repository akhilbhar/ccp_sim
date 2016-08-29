package participant

import java.util.GregorianCalendar

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.{MarketFactors, MarketFactorsBuilder}
import model.{Portfolio, Position}
import participant.Client._
import pricer.{PortfolioPricer, PortfolioPricingError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.\/
import scalaz.std.{FutureInstances, OptionInstances}

/**
  * Client.
  * @param name name of the client.
  */
case class Client(name: String) extends Actor with FutureInstances with OptionInstances {
  private var portfolio: Portfolio = Portfolio(List())

  override def receive: Receive = {
    case Value(factors) => pipe(value(factors)) to sender
    case MonteCarlo(builder) => pipe(monteCarlo(builder)) to sender
    case AddPosition(p) => addPosition(p)
  }

  private def value(implicit factors: MarketFactors): Future[PortfolioPricingError \/ Double] = {
    PortfolioPricer.price(portfolio)
  }

  private def addPosition(position: Position): Unit = {
    portfolio = portfolio.addPosition(position)
    println("Added " + position.instrument)
  }

  private def monteCarlo(builder: MarketFactorsBuilder): Future[PortfolioPricingError \/ Double] = {
    val date = new GregorianCalendar(2016, 0, 29)

    implicit val materializer = ActorMaterializer()

    val marketFactors = builder
      .oneDayForecastMarketFactors(portfolio, date)(MarketFactorsParameters(horizon = 3000))
      .flatMap(_.factors.toMat(Sink.head)(Keep.right).run)

    marketFactors.flatMap(value(_))
  }
}

object Client {

  def props(name: String): Props =
    Props(Client(name))

  case class AddPosition(position: Position)
  case class Value(factors: MarketFactors)
  case class MonteCarlo(builder: MarketFactorsBuilder)
}
