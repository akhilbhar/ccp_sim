import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import data.YahooHistoricalDataFetcher
import marketFactor.HistoricalMarketFactorsBuilder
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.Equity
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

  val actorSystem = ActorSystem("Default")

  val builder = HistoricalMarketFactorsBuilder(YahooHistoricalDataFetcher)
  val parameters = new MarketFactorsParameters

//  val buyer = actorSystem.actorOf(Props(wire[Client]))
//  val seller = actorSystem.actorOf(Props(wire[Client]))
  val buyer = actorSystem.actorOf(Client.props("Dennis", builder, parameters))
  val seller = actorSystem.actorOf(Client.props("Saskia", builder, parameters))

  val apple = Equity("AAPL")
  val google = Equity("GOOG")
  val microsoft = Equity("MSFT")

  BilateralClearingEngine.performTransaction(apple, 1, buyer, seller)
  BilateralClearingEngine.performTransaction(google, 1, buyer, seller)
  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)
  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)
  BilateralClearingEngine.performTransaction(microsoft, 1, seller, buyer)

  implicit val timeout = Timeout(60 seconds)

  ask(buyer, Client.Value).mapTo[PortfolioPricingError \/ Double] onComplete {
    case Success(v) => println(s"Value: $v")
    case Failure(e) => println(s"Error $e")
  }

  ask(buyer, Client.ValueAtRisk(0.05, 10000))
    .mapTo[List[PortfolioPricingError] \/ Double] onComplete {
    case Success(v) => println(s"VaR: $v")
    case Failure(e) => println(s"Error: $e")
  }
}
