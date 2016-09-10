package valueAtRisk

import java.util.Calendar

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Keep, Merge, Sink}
import akka.stream.{ActorMaterializer, FlowShape}
import breeze.numerics.round
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.{MarketFactors, MarketFactorsBuilder}
import model.{Portfolio, PortfolioError}
import valueAtRisk.OneDayValueAtRiskCalculator.VaR

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

/**
  * Distributed computation of the value at risk using a Monte Carlo simulation.
  *
  * @param thresholdLoss probability of loss
  * @param simulations Real of simulations to perform
  * @param builder builder for estimating
  * @param parameters parameters for adapting the market factors
  * @param system actor system on which the computation has to be run
  * @param clusterSize size of the actor cluster for computation
  */
case class OneDayValueAtRiskCalculator(thresholdLoss: Double, simulations: Long)(
    implicit builder: MarketFactorsBuilder,
    parameters: MarketFactorsParameters,
    system: ActorSystem,
    clusterSize: Int = 10) {

  /**
    * Run the the VaR computation.
    * @param portfolio portfolio to analyze
    * @param date the date of the analysis
    * @return the VaR and the outcome of each scenario or a list of errors.
    */
  def run(portfolio: Portfolio, date: Calendar): Future[ValueAtRiskError \/ VaR] = {
    implicit val materializer = ActorMaterializer()

    val sourceF =
      builder.oneDayForecastMarketFactors(portfolio, date).map(_.factors)

    val resultsF =
      sourceF
        .flatMap(
          _.take(simulations)
            .via(balancer(simulateOneDayPrice(portfolio), clusterSize))
            .toMat(Sink.seq)(Keep.right)
            .run())
        .flatMap(Future.sequence(_))
        .map(_.toList)

    val sortedResultsF: Future[(List[PortfolioError], List[Double])] =
      resultsF.map(_.separate).map(t => { (t._1, t._2.sorted) })

    sortedResultsF.map({
      case t if t._1.isEmpty =>
        \/-(VaR(t._2(round(thresholdLoss * simulations).toInt - 1), t._2))
      case t => -\/(PortfolioPricingError(t._1))
    })
  }

  /**
    * Flow to balance the work between different a fixed Real of workers.
    *
    * @param worker worker that computes one simulation.
    * @param workerCount Real of workers.
    * @tparam In input of the flow
    * @tparam Out output of the flow
    * @return Flow outputting all the computed scenarios
    */
  private def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Flow[In, Out, NotUsed] = {
    import akka.stream.scaladsl.GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer =
        b.add(Balance[In](workerCount, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](workerCount))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }

  /**
    * Takes market factors as input and uses them to compute the value of the portfolio.
    *
    * @param portfolio portfolio to be valued.
    * @return value of the portfolio or an error.
    */
  private def simulateOneDayPrice(portfolio: Portfolio) = Flow[MarketFactors].map(portfolio.price(_, parameters))

}

object OneDayValueAtRiskCalculator {
  case class VaR(valueAtRisk: Double, outcomes: List[Double])
}

sealed trait ValueAtRiskError
case class PortfolioPricingError(errors: List[PortfolioError]) extends ValueAtRiskError
