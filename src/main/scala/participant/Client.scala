package participant

import java.util.Calendar

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.pipe
import com.softwaremill.macwire._
import custodian.Position
import marketFactor.MarketFactorsBuilder
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.{Portfolio, PortfolioError}
import participant.Client._
import valueAtRisk.{OneDayValueAtRiskCalculator, ValueAtRiskError}
import valueAtRisk.OneDayValueAtRiskCalculator.VaR

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.\/
import scalaz.std.{FutureInstances, OptionInstances}

/**
  * Client.
  * @param name name of the client.
  */
case class Client(name: String,
                  builder: MarketFactorsBuilder,
                  parameters: MarketFactorsParameters)
    extends Actor
    with FutureInstances
    with OptionInstances {
  private var portfolio: Portfolio = Portfolio(List())

  override def receive: Receive = {
    case Value => pipe(value) to sender
    case ValueAtRisk(thresholdLoss, simulations) =>
      pipe(valueAtRisk(thresholdLoss, simulations)) to sender
    case AddPosition(p) => addPosition(p)
    case MarkToMarket => pipe(markToMarket) to sender
  }

  private def value: Future[\/[PortfolioError, Double]] = {
    val date = Calendar.getInstance

    portfolio.price(builder.marketFactors(date)(parameters), parameters)
  }

  private def addPosition(position: Position): Unit = {
    portfolio = portfolio.addPosition(position)
  }

  private def valueAtRisk(
      thresholdLoss: Double,
      simulations: Long): Future[ValueAtRiskError \/ VaR] = {
    val clusterSize = 100

    val now = Calendar.getInstance()

    OneDayValueAtRiskCalculator(thresholdLoss, simulations)(
      builder,
      parameters,
      ActorSystem(),
      clusterSize).run(portfolio, now)
  }

  private def markToMarket: Future[\/[PortfolioError, Double]] = {
    val date = Calendar.getInstance

    portfolio.markToMarket(builder.marketFactors(date)(parameters))
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
  case object MarkToMarket
}
