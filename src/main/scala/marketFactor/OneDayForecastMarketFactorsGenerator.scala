package marketFactor
import java.time.LocalDate

import akka.NotUsed
import akka.stream.scaladsl.Source
import marketFactor.OneDayForecastMarketFactorsGenerator.CurrentFactors
import model.Equity
import org.apache.commons.math3.random.{CorrelatedRandomVectorGenerator, GaussianRandomGenerator, JDKRandomGenerator}
import org.apache.commons.math3.stat.correlation.Covariance

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by dennis on 20/8/16.
  */
case class OneDayMarketForecastFactorsGenerator(date: LocalDate,
                                                currentFactors: Map[Equity, CurrentFactors])
    extends MarketFactorsGenerator {
  private val nextDay = date.plusDays(1)

  private def generator = {
    val randomGenerator = new JDKRandomGenerator
    val gaussianRandomGenerator = new GaussianRandomGenerator(randomGenerator)
    val priceHistory = currentFactors.values.map(_.priceHistory.toArray).toArray
    val covariance = new Covariance(priceHistory)
    val covarianceMatrix = covariance.getCovarianceMatrix

    new CorrelatedRandomVectorGenerator(covarianceMatrix,
                                        1.0e-12 * covarianceMatrix.getNorm,
                                        gaussianRandomGenerator)
  }

  case class GeneratedMarketFactors(generatedPrices: Map[Equity, Double]) extends MarketFactors {
    override protected def price(equity: Equity): Option[Double] = generatedPrices.get(equity)

    override protected def volatility(equity: Equity): Option[Double] =
      currentFactors.get(equity).map(_.volatility)
  }

  override def factors: Source[Future[MarketFactors], NotUsed] = {
    Source
      .repeat(generator)
      .map(random =>
        Future({
          val correlated = currentFactors.keys zip random.nextVector()

          val generatedPrice: Map[Equity, Double] = correlated.toSeq.map({
            case (equity, randomValue) =>
              (equity,
               currentFactors(equity).price + randomValue * currentFactors(equity).volatility)
          })(scala.collection.breakOut)

          GeneratedMarketFactors(generatedPrice)
        }))
  }
}

object OneDayForecastMarketFactorsGenerator {
  case class CurrentFactors(price: Double, volatility: Double, priceHistory: Vector[Double])
}
