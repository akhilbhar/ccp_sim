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
  //  val fifty = 50
  //  val hundred = 100
  //  implicit val timeout = Timeout(5 seconds)
  //
  //  val system = ActorSystem("Default")
  //  val dennis = system.actorOf(Client.props("Dennis"))
  //  val dan = system.actorOf(Client.props("Dan"))
  //
  //  val issuerService = system.actorOf(IssuerService.props("Service"))
  //  val market = system.actorOf(Market.props("Market"))
  //
  //  val microsoft: Issuer = Await.result(
  //      ask(issuerService, AddIssuer("Apple", Uniform(hundred, hundred)))
  //        .mapTo[Issuer],
  //      Duration.Inf)
  //
  //  val apple = Await.result(
  //      ask(issuerService, AddIssuer("Microsoft", Uniform(fifty, fifty)))
  //        .mapTo[Issuer],
  //      Duration.Inf)
  //
  //  val contract = Await.result(
  //      ask(market, AddContract(apple, dennis, dan)).mapTo[Equity],
  //      Duration.Inf)
  //
  //  val price = Await.result(contract.price, Duration.Inf)
  //
  //  println(price)

  TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

  val actorSystem = ActorSystem("Default")

  val builder = HistoricalMarketFactorsBuilder(YahooHistoricalDataFetcher)
  val parameters = MarketFactorsParameters(horizon = 5)

//  val buyer = actorSystem.actorOf(Props(wire[Client]))
//  val seller = actorSystem.actorOf(Props(wire[Client]))
  val buyer = actorSystem.actorOf(Client.props("Dennis", builder, parameters))
  val seller = actorSystem.actorOf(Client.props("Saskia", builder, parameters))

  val apple = Equity("AAPL")
  val appleMaturity = new GregorianCalendar(2016, 8, 30)
  val appleCall = CallOption(apple, 95, appleMaturity)

  val google = Equity("GOOG")
  val microsoft = Equity("MSFT")
//  val microsoftPut = PutOption(microsoft, )
  val tesla = Equity("TSLA")
  val teslaCall = CallOption(tesla, 220, new GregorianCalendar(2016, 8, 7))

//  BilateralClearingEngine.performTransaction(apple, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(google, 1, buyer, seller)
//  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)
  BilateralClearingEngine.performTransaction(appleCall, 1, buyer, seller)


  //  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)
//  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)
//  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)

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
