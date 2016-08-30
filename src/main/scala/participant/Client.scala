package participant

import java.util.{Calendar, GregorianCalendar}

import `var`.ValueAtRiskCalculator
import `var`.ValueAtRiskCalculator.VaR
import akka.actor.{Actor, ActorSystem, Props}
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
    case ValueAtRisk(t, d, s, b) => pipe(valueAtRisk(t, d, s, b)) to sender
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

  private def valueAtRisk(
      thresholdLoss: Double,
      date: Calendar,
      simulation: Long,
      builder: MarketFactorsBuilder): Future[List[PortfolioPricingError] \/ VaR] = {
    ValueAtRiskCalculator(thresholdLoss, simulation)(builder, 100, ActorSystem())
      .run(portfolio, date)
  }

  private def monteCarlo(builder: MarketFactorsBuilder): Future[PortfolioPricingError \/ Double] = {
    val date = new GregorianCalendar(2016, 0, 29)

    implicit val materializer = ActorMaterializer()

    val marketFactors = builder
      .oneDayForecastMarketFactors(portfolio, date)(MarketFactorsParameters(horizon = 5))
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
  case class ValueAtRisk(thresholdLoss: Double,
                         date: Calendar,
                         simulation: Long,
                         builder: MarketFactorsBuilder)
}
