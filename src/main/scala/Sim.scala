import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import data.YahooHistoricalDataFetcher
import marketFactor.HistoricalMarketFactorsBuilder
import model.Equity
import participant.{BilateralClearingEngine, Client}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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
  val marketFactorsBuilder = HistoricalMarketFactorsBuilder(YahooHistoricalDataFetcher)
  val name = "Dennis"

//  val buyer = actorSystem.actorOf(Props(wire[Client]))
//  val seller = actorSystem.actorOf(Props(wire[Client]))
  val buyer = actorSystem.actorOf(Client.props("Dennis", marketFactorsBuilder))
  val seller = actorSystem.actorOf(Client.props("Saskia", marketFactorsBuilder))
  val instrument = Equity("AAPL")

  BilateralClearingEngine.performTransaction(instrument, 1, buyer, seller)

  implicit val timeout = Timeout(5 seconds)

  ask(buyer, Client.Value).mapTo[Double] onComplete {
    case Success(v) => println("Value" + v)
    case Failure(e) => println(e.getMessage)
  }

//  val fo = YahooHistoricalDataFetcher.historicalPrices(Equity("APPL"), LocalDate.now().minusDays(10), LocalDate.now())
//
//  fo onComplete {
//    case Success(o) => println(o.getOrElse("Fuck"))
//    case Failure(e) => println("BLBLALBLABLA" + e.getMessage)
//  }

//  val from = Calendar.getInstance();
//  val to = Calendar.getInstance();
//  from.add(Calendar.DATE, -2); // from 5 years ago
//
//  val google = YahooFinance.get("GOOG", from, to, Interval.DAILY);
//
//  println(google.getCurrency)
}
