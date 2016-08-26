import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import model.Equity

import scala.concurrent.duration._

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

  val apple: Equity = Equity("AAPL")

  val gen: OneDayMarketFactorsGenerator = OneDayMarketFactorsGenerator(
    LocalDate.now(),
    Map((apple, CurrentFactors(100, 10, Vector(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)))))

  implicit val system = ActorSystem("QuickStart")
  implicit val timeout = Timeout(5 seconds)

  implicit val materializer = ActorMaterializer()
  gen.factors.runForeach(println(_))
}
