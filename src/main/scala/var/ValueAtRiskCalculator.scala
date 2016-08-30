package `var`

import java.util.Calendar

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Keep, Merge, Sink}
import akka.stream.{ActorMaterializer, FlowShape}
import breeze.numerics.round
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.{MarketFactors, MarketFactorsBuilder}
import model.Portfolio
import pricer.{PortfolioPricer, PortfolioPricingError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-, _}

/**
  * Created by dennis on 30/8/16.
  */
case class ValueAtRiskCalculator(thresholdLoss: Double, simulations: Long)(
    implicit builder: MarketFactorsBuilder,
    clusterSize: Int = 10,
    system: ActorSystem) {
  def run(portfolio: Portfolio, date: Calendar): Future[List[PortfolioPricingError] \/ Double] = {
    implicit val materializer = ActorMaterializer()

    val resultsF = builder
      .oneDayForecastMarketFactors(portfolio, date)(MarketFactorsParameters(horizon = 3000))
      .flatMap(
        _.factors
          .take(simulations)
          .via(balancer(worker(portfolio), clusterSize))
          .toMat(Sink.seq)(Keep.right)
          .run())
      .flatMap(Future.sequence(_))
      .map(_.toList)

    val groupedResultsF = resultsF
      .map(_.separate)
      .map(t => { (t._1, t._2.sorted) })

    groupedResultsF.map({
      case t if t._1.isEmpty => \/-(t._2(round(thresholdLoss * simulations).asInstanceOf[Int]))
      case t => -\/(t._1)
    })
  }

  private def balancer[In, Out](worker: Flow[In, Out, Any],
                                workerCount: Int): Flow[In, Out, NotUsed] = {
    import akka.stream.scaladsl.GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](workerCount))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }

  private def worker(portfolio: Portfolio) =
    Flow[MarketFactors].map(implicit factors => {
      PortfolioPricer.price(portfolio)
    })
}
