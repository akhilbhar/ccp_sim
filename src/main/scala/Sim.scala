import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import breeze.stats.distributions.Uniform
import model.Market.AddContract
import model.{Equity, Market}
import issuer.IssuerService.AddIssuer
import issuer.{Issuer, IssuerService}
import player._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by dennis on 19/6/16.
  */
object Sim extends App {
  val fifty = 50
  val hundred = 100
  implicit val timeout = Timeout(5 seconds)

  val system = ActorSystem("Default")
  val dennis = system.actorOf(Client.props("Dennis"))
  val dan = system.actorOf(Client.props("Dan"))

  val issuerService = system.actorOf(IssuerService.props("Service"))
  val market = system.actorOf(Market.props("Market"))

  val microsoft: Issuer = Await.result(
      ask(issuerService, AddIssuer("Apple", Uniform(hundred, hundred)))
        .mapTo[Issuer],
      Duration.Inf)

  val apple = Await.result(
      ask(issuerService, AddIssuer("Microsoft", Uniform(fifty, fifty)))
        .mapTo[Issuer],
      Duration.Inf)

  val contract = Await.result(
      ask(market, AddContract(apple, dennis, dan)).mapTo[Equity],
      Duration.Inf)

  val price = Await.result(contract.price, Duration.Inf)

  println(price)
}
