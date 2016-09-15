import java.util.{GregorianCalendar, TimeZone}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import data.GoogleSource
import instrument.Equity
import instrument.EquityOption.Call
import instrument.option.EuroEquityOption
import marketFactor.HistoricalMarketFactorsBuilder
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.PortfolioError
import participant.{BilateralClearingEngine, Client}
import pricingModel.equityOption.{Binomial, BlackSholes}
import valueAtRisk.ValueAtRiskCalculator.VaR
import valueAtRisk.ValueAtRiskError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalaz.\/


/**
  * Created by dennis on 19/6/16.
  */
object Sim extends App {
  TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

  val actorSystem = ActorSystem("Default")

  val builder = HistoricalMarketFactorsBuilder
  val parameters = MarketFactorsParameters(horizon = 1000)

  val buyer = actorSystem.actorOf(Client.props("Dennis", builder, parameters))
  val seller = actorSystem.actorOf(Client.props("Saskia", builder, parameters))

  val apple = Equity("AAPL")
  val appleMaturity = new GregorianCalendar(2016, 8, 16)
  val appleCall = EuroEquityOption(Call, apple, 90, appleMaturity)(model = BlackSholes)

  val google = Equity("GOOG")
  val microsoft = Equity("MSFT")

  val tesla = Equity("TSLA")
  val teslaCall = EuroEquityOption(Call, tesla, 20, new GregorianCalendar(2016, 8, 16))

  val ibm = Equity("IBM")
  val ibmCall = EuroEquityOption(Call, ibm, 100, new GregorianCalendar(2018, 0, 19))

  val idt = Equity("IDT")
  val idtCall = EuroEquityOption(Call, idt, 12.5, new GregorianCalendar(2016, 8, 16))

//  BilateralClearingEngine.performTransaction(apple, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(ibm, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(Equity("BRK-B"), 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(Equity("NFLX"), 1, buyer, seller)
  BilateralClearingEngine.performTransaction(appleCall, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(teslaCall, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(ibmCall, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(idtCall, 1, buyer, seller)

  Thread.sleep(2000)

  implicit val timeout = Timeout(600 seconds)

  ask(buyer, Client.MarkToMarket).mapTo[PortfolioError \/ Double] onComplete {
    case Success(v) => println(s"MtM: $v")
    case Failure(e) => println("Error: " + e)
  }

  ask(buyer, Client.Value).mapTo[PortfolioError \/ Double] onComplete {
    case Success(v) => println(s"Value: $v")
    case Failure(e) => println("Error: " + e)
  }

//  ask(buyer, Client.ValueAtRisk(0.01, 10000))
//    .mapTo[ValueAtRiskError \/ VaR] onComplete {
//    case Success(v) => println("VaR: " + v.map(_.valueAtRisk))
//    case Failure(e) => {
//      println("Error: " + e)
//      e.getStackTrace.foreach(println(_))
//    }
//  }

//  ask(buyer, Client.ValueAtRisk(0.01, 10000))
//    .mapTo[Option[VaR]] onComplete {
//    case Success(v) => println("VaR: " + v.map(_.valueAtRisk))
//    case Failure(e) => {
//      println("Error: " + e)
//      e.getStackTrace.foreach(println(_))
//    }
//  }
//

//  ibm.historicalPrices(new GregorianCalendar(2016, 1, 4), new GregorianCalendar(2016, 2, 4)).onComplete({
//    case Success(v) => println(v.map(_.head.adjusted))
//    case Failure(e) => println(e)
//  })
//
//  ibm.historicalPrices(new GregorianCalendar(2016, 1, 4), new GregorianCalendar(2016, 2, 4)).onComplete({
//    case Success(v) => println(v.map(_.head.adjusted))
//    case Failure(e) => println(e)
//  })
}
