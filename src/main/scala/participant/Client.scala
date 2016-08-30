package participant

import java.util.Calendar

import `var`.OneDayValueAtRiskCalculator
import `var`.OneDayValueAtRiskCalculator.VaR
import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.pipe
import com.softwaremill.macwire._
import marketFactor.MarketFactorsBuilder
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
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
case class Client(name: String, builder: MarketFactorsBuilder, parameters: MarketFactorsParameters)
    extends Actor
    with FutureInstances
    with OptionInstances {
  private var portfolio: Portfolio = Portfolio(List())

  override def receive: Receive = {
    case Value => pipe(value) to sender
    case ValueAtRisk(thresholdLoss, simulations) =>
      pipe(valueAtRisk(thresholdLoss, simulations)) to sender
    case AddPosition(p) => addPosition(p)
  }

  private def value: Future[PortfolioPricingError \/ Double] = {
    PortfolioPricer.price(portfolio)(builder.marketFactors(Calendar.getInstance)(parameters))
  }

  private def addPosition(position: Position): Unit = {
    portfolio = portfolio.addPosition(position)
    println("Added " + position.instrument)
  }

  private def valueAtRisk(thresholdLoss: Double,
                          simulations: Long): Future[List[PortfolioPricingError] \/ VaR] = {
    val clusterSize = 100

    OneDayValueAtRiskCalculator(thresholdLoss, simulations)(
      builder,
      parameters,
      ActorSystem(),
      clusterSize).run(portfolio, Calendar.getInstance)
  }
}

object Client {
  def props(name: String,
            builder: MarketFactorsBuilder,
            parameters: MarketFactorsParameters): Props =
    Props(wire[Client])

  case class AddPosition(position: Position)
  case object Value
  case class ValueAtRisk(thresholdLoss: Double, simulations: Long)
}
