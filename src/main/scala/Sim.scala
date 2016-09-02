import java.util.{GregorianCalendar, TimeZone}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import data.YahooHistoricalDataFetcher
import marketFactor.HistoricalMarketFactorsBuilder
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.{CallOption, Equity}
import participant.{BilateralClearingEngine, Client}
import pricer.PortfolioPricingError

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

  val builder = HistoricalMarketFactorsBuilder(YahooHistoricalDataFetcher)
  val parameters = MarketFactorsParameters(horizon = 1000)

//  val buyer = actorSystem.actorOf(Props(wire[Client]))
//  val seller = actorSystem.actorOf(Props(wire[Client]))
  val buyer = actorSystem.actorOf(Client.props("Dennis", builder, parameters))
  val seller = actorSystem.actorOf(Client.props("Saskia", builder, parameters))

  val apple = Equity("AAPL")
  val appleMaturity = new GregorianCalendar(2016, 8, 16)
  val appleCall = CallOption(apple, 90, appleMaturity)

  val google = Equity("GOOG")
  val microsoft = Equity("MSFT")

  val tesla = Equity("TSLA")
  val teslaCall = CallOption(tesla, 185, new GregorianCalendar(2017, 2, 17))

  val ibm = Equity("IBM")
  val ibmCall = CallOption(ibm, 100, new GregorianCalendar(2018, 0, 19))

  val foo = Equity("sdf")
  val foo

//  BilateralClearingEngine.performTransaction(apple, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(google, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)
//  BilateralClearingEngine.performTransaction(appleCall, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(teslaCall, 1, buyer, seller)
  BilateralClearingEngine.performTransaction(ibmCall, 1, buyer, seller)

  implicit val timeout = Timeout(600 seconds)

  ask(buyer, Client.Value).mapTo[PortfolioPricingError \/ Double] onComplete {
    case Success(v) => println(s"Value: $v")
    case Failure(e) => println(s"Error $e")
  }

//  ask(buyer, Client.ValueAtRisk(0.05, 10000))
//    .mapTo[List[PortfolioPricingError] \/ VaR] onComplete {
//    case Success(v) => println("VaR: " + v)
//    case Failure(e) => {
//      println("Error: " + e)
//      e.getStackTrace.foreach(println(_))
//    }
//  }
}
