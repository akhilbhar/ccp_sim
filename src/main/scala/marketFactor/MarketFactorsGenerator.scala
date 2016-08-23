package marketFactor

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.concurrent.Future

/**
  * Created by dennis on 16/8/16.
  */
trait MarketFactorsGenerator {
  def factors: Source[Future[MarketFactors], NotUsed]

//  abstract class GeneratedMarketFactors(generatedPrice: Map[Equity, Double]) extends MarketFactors
}

//object FixedFactorsGenerator {
//  case class FixedFactors(price: Double, volatility: Double)
//}

//class FixedFactorsGenerator(fixedFactors: Map[Equity, FixedFactors]) extends FactorsGenerator {
//  case class GeneratedMarketFactors(fixedPrices: Map[Equity, Double]) extends Factors {
//    override protected def price(equity: Equity): Option[Double] =
//      fixedPrices.get(equity)
//
//    override protected def volatility(equity: Equity): Option[Double] =
//      fixedFactors.get(equity).map(_.volatility)
//  }
//
//  def factors: Process[Task, Factors] = {
//    Process.repeatEval({
//      Task.delay({
//        val fixedPrices: Map[Equity, Double] = fixedFactors.map({
//          case (equity, fixedFactor) => (equity, fixedFactor.price)
//        })
//
//        GeneratedMarketFactors(fixedPrices)
//      })
//    })
//  }
//}
